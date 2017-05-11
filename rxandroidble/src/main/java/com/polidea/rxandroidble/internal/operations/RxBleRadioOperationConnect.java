package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.DeadObjectException;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.BluetoothGattProvider;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.BleConnectionCompat;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Emitter;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Cancellable;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;

import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTED;
import static com.polidea.rxandroidble.internal.DeviceModule.CONNECT_TIMEOUT;

public class RxBleRadioOperationConnect extends RxBleRadioOperation<BluetoothGatt> {

    public static class Builder {

        private final BluetoothDevice bluetoothDevice;
        private final BleConnectionCompat connectionCompat;
        private final RxBleGattCallback rxBleGattCallback;
        private final BluetoothGattProvider bluetoothGattProvider;
        private final TimeoutConfiguration connectTimeout;
        private boolean autoConnect = false;

        @Inject
        public Builder(
                BluetoothDevice bluetoothDevice,
                BleConnectionCompat connectionCompat,
                RxBleGattCallback rxBleGattCallback,
                @Named(CONNECT_TIMEOUT) TimeoutConfiguration connectionTimeout,
                BluetoothGattProvider bluetoothGattProvider) {
            this.bluetoothDevice = bluetoothDevice;
            this.connectionCompat = connectionCompat;
            this.rxBleGattCallback = rxBleGattCallback;
            this.bluetoothGattProvider = bluetoothGattProvider;
            this.connectTimeout = connectionTimeout;
        }

        public Builder setAutoConnect(boolean autoConnect) {
            this.autoConnect = autoConnect;
            return this;
        }

        public RxBleRadioOperationConnect build() {
            return new RxBleRadioOperationConnect(bluetoothDevice, connectionCompat, rxBleGattCallback, bluetoothGattProvider,
                    connectTimeout, autoConnect);
        }
    }

    private final BluetoothDevice bluetoothDevice;
    private final BleConnectionCompat connectionCompat;
    private final RxBleGattCallback rxBleGattCallback;
    private final BluetoothGattProvider bluetoothGattProvider;
    private final TimeoutConfiguration connectTimeout;
    private final boolean autoConnect;

    private final BehaviorSubject<Boolean> isSubscribed = BehaviorSubject.create();

    private final Observable<BluetoothGatt> operationConnectAsObservableWithSubscribersMonitoring = super.asObservable()
            .doOnSubscribe(new Action0() {
                @Override
                public void call() {
                    isSubscribed.onNext(true);
                }
            })
            .doOnUnsubscribe(new Action0() {
                @Override
                public void call() {
                    isSubscribed.onNext(false);
                }
            })
            .share();

    @Inject
    RxBleRadioOperationConnect(
            BluetoothDevice bluetoothDevice,
            BleConnectionCompat connectionCompat,
            RxBleGattCallback rxBleGattCallback,
            BluetoothGattProvider bluetoothGattProvider,
            @Named(CONNECT_TIMEOUT) TimeoutConfiguration connectTimeout,
            boolean autoConnect) {
        this.bluetoothDevice = bluetoothDevice;
        this.connectionCompat = connectionCompat;
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGattProvider = bluetoothGattProvider;
        this.connectTimeout = connectTimeout;
        this.autoConnect = autoConnect;
    }

    @Override
    public Observable<BluetoothGatt> asObservable() {
        return operationConnectAsObservableWithSubscribersMonitoring;
    }

    @Override
    protected void protectedRun() {
        getConnectedBluetoothGatt()
                .compose(wrapWithTimeoutWhenNotAutoconnecting())
                // when there are no subscribers there is no point of continuing work -> next will be disconnect operation
                .takeUntil(asObservableHasNoSubscribers().doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean noSubscribers) {
                        RxBleLog.d("No subscribers, finishing operation");
                    }
                }))
                .doOnNext(new Action1<BluetoothGatt>() {
                    @Override
                    public void call(BluetoothGatt ignored) {
                        isSubscribed.onCompleted();
                    }
                })
                .subscribe(getSubscriber());

        if (autoConnect) {
            // with autoConnect the connection may be established after a really long time
            releaseRadio();
        }
    }

    private Observable.Transformer<BluetoothGatt, BluetoothGatt> wrapWithTimeoutWhenNotAutoconnecting() {
        return new Observable.Transformer<BluetoothGatt, BluetoothGatt>() {
            @Override
            public Observable<BluetoothGatt> call(Observable<BluetoothGatt> bluetoothGattObservable) {
                return autoConnect
                        ? bluetoothGattObservable
                        : bluetoothGattObservable
                        .timeout(connectTimeout.timeout, connectTimeout.timeoutTimeUnit,
                                prepareConnectionTimeoutErrorObservable(), connectTimeout.timeoutScheduler);
            }
        };
    }

    @NonNull
    private Observable<BluetoothGatt> prepareConnectionTimeoutErrorObservable() {
        return Observable.fromCallable(new Func0<BluetoothGatt>() {
            @Override
            public BluetoothGatt call() {
                throw new BleGattCallbackTimeoutException(bluetoothGattProvider.getBluetoothGatt(), BleGattOperationType.CONNECTION_STATE);
            }
        });
    }

    @NonNull
    private Observable<Boolean> asObservableHasNoSubscribers() {
        return isSubscribed.filter(new Func1<Boolean, Boolean>() {
            @Override
            public Boolean call(Boolean aBoolean) {
                return !aBoolean;
            }
        });
    }

    /**
     * Emits BluetoothGatt and completes after connection is established.
     *
     * @return BluetoothGatt after connection reaches {@link com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState#CONNECTED}
     * state.
     * @throws com.polidea.rxandroidble.exceptions.BleDisconnectedException if connection was disconnected/failed before it was established.
     */
    @NonNull
    private Observable<BluetoothGatt> getConnectedBluetoothGatt() {
        // start connecting the BluetoothGatt
        // note: Due to different Android BLE stack implementations it is not certain whether `connectGatt()` or `BluetoothGattCallback`
        // will emit BluetoothGatt first
        return Observable.create(
                new Action1<Emitter<BluetoothGatt>>() {
                    @Override
                    public void call(Emitter<BluetoothGatt> emitter) {
                        final Subscription connectedBluetoothGattSubscription = Observable.fromCallable(new Func0<BluetoothGatt>() {
                            @Override
                            public BluetoothGatt call() {
                                return bluetoothGattProvider.getBluetoothGatt();
                            }
                        })
                                // when the connected state will be emitted bluetoothGattProvider should contain valid Gatt
                                .delaySubscription(
                                        rxBleGattCallback
                                                .getOnConnectionStateChange()
                                                .takeFirst(
                                                        new Func1<RxBleConnection.RxBleConnectionState, Boolean>() {
                                                            @Override
                                                            public Boolean call(RxBleConnection.RxBleConnectionState rxBleConnectionState) {
                                                                return rxBleConnectionState == CONNECTED;
                                                            }
                                                        }
                                                )
                                )
                                // disconnect may happen even if the connection was not established yet
                                .mergeWith(rxBleGattCallback.<BluetoothGatt>observeDisconnect())
                                .subscribe(emitter);

                        emitter.setCancellation(new Cancellable() {
                            @Override
                            public void cancel() throws Exception {
                                connectedBluetoothGattSubscription.unsubscribe();
                            }
                        });

                        /*
                        * Apparently the connection may be established fast enough to introduce a race condition so the subscription
                        * must be established first before starting the connection.
                        * https://github.com/Polidea/RxAndroidBle/issues/178
                        * */

                        final BluetoothGatt bluetoothGatt = connectionCompat
                                .connectGatt(bluetoothDevice, autoConnect, rxBleGattCallback.getBluetoothGattCallback());
                        /*
                        * Update BluetoothGatt when connection is initiated. It is not certain
                        * if this or RxBleGattCallback.onConnectionStateChange will be first.
                        * */
                        bluetoothGattProvider.updateBluetoothGatt(bluetoothGatt);
                    }
                },
                Emitter.BackpressureMode.NONE
        );
    }

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return new BleDisconnectedException(deadObjectException, bluetoothDevice.getAddress());
    }
}
