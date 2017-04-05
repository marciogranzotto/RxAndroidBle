package com.polidea.rxandroidble.internal;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.exceptions.BleAlreadyConnectedException;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Cancellable;
import rx.functions.Func0;
import rx.internal.subscriptions.CancellableSubscription;
import rx.subjects.BehaviorSubject;

import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTED;
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTING;
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.DISCONNECTED;

@DeviceScope
class RxBleDeviceImpl implements RxBleDevice {

    private final BluetoothDevice bluetoothDevice;
    private final RxBleConnection.Connector connector;
    private final BehaviorSubject<RxBleConnection.RxBleConnectionState> connectionStateSubject = BehaviorSubject.create(DISCONNECTED);
    private AtomicBoolean isConnected = new AtomicBoolean(false);

    @Inject
    public RxBleDeviceImpl(BluetoothDevice bluetoothDevice, RxBleConnection.Connector connector) {
        this.bluetoothDevice = bluetoothDevice;
        this.connector = connector;
    }

    @Override
    public Observable<RxBleConnection.RxBleConnectionState> observeConnectionStateChanges() {
        return connectionStateSubject.distinctUntilChanged();
    }

    @Override
    public RxBleConnection.RxBleConnectionState getConnectionState() {
        return observeConnectionStateChanges().toBlocking().first();
    }

    @Override
    @Deprecated
    public Observable<RxBleConnection> establishConnection(Context context, boolean autoConnect) {
        return establishConnection(autoConnect);
    }

    @Override
    public Observable<RxBleConnection> establishConnection(final boolean autoConnect) {
        return Observable.defer(new Func0<Observable<RxBleConnection>>() {
            @Override
            public Observable<RxBleConnection> call() {

                if (isConnected.compareAndSet(false, true)) {
                    return connector.prepareConnection(autoConnect)
                            .doOnSubscribe(new Action0() {
                                @Override
                                public void call() {
                                    connectionStateSubject.onNext(CONNECTING);
                                }
                            })
                            .doOnNext(new Action1<RxBleConnection>() {
                                @Override
                                public void call(RxBleConnection rxBleConnection) {
                                    connectionStateSubject.onNext(CONNECTED);
                                }
                            })
                            .doOnUnsubscribe(new Action0() {
                                @Override
                                public void call() {
                                    connectionStateSubject.onNext(DISCONNECTED);
                                    isConnected.set(false);
                                }
                            });
                } else {
                    return Observable.error(new BleAlreadyConnectedException(bluetoothDevice.getAddress()));
                }
            }
        });
    }

    @Override
    public String getName() {
        return bluetoothDevice.getName();
    }

    @Override
    public String getMacAddress() {
        return bluetoothDevice.getAddress();
    }

    @Override
    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    @Override
    public Observable<Integer> bond(final Context context) {
        return Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(final Subscriber<? super Integer> subscriber) {

                final int bondState = bluetoothDevice.getBondState();
                if (bondState == BluetoothDevice.BOND_BONDED) {
                    subscriber.onNext(bondState);
                    subscriber.onCompleted();
                    return;
                }


                final BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();

                        if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                            int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                            if (state == BluetoothDevice.BOND_BONDED) {
                                if (!subscriber.isUnsubscribed()) {
                                    subscriber.onNext(state);
                                    subscriber.onCompleted();
                                }
                            }
                        }
                    }
                };

                context.registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));

                bluetoothDevice.createBond();

                subscriber.add(new CancellableSubscription(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        context.unregisterReceiver(receiver);
                    }
                }));
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RxBleDeviceImpl)) {
            return false;
        }

        RxBleDeviceImpl that = (RxBleDeviceImpl) o;
        return bluetoothDevice.equals(that.bluetoothDevice);
    }

    @Override
    public int hashCode() {
        return bluetoothDevice.hashCode();
    }

    @Override
    public String toString() {
        return "RxBleDeviceImpl{" + "bluetoothDevice=" + bluetoothDevice.getName() + '(' + bluetoothDevice.getAddress() + ')' + '}';
    }
}
