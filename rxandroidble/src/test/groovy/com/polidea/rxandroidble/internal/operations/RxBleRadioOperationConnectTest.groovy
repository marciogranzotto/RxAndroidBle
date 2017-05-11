package com.polidea.rxandroidble.internal.operations

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException
import com.polidea.rxandroidble.internal.connection.BluetoothGattProvider
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble.internal.util.BleConnectionCompat
import com.polidea.rxandroidble.internal.util.MockOperationTimeoutConfiguration
import java.util.concurrent.TimeUnit
import rx.Subscription
import rx.observers.TestSubscriber
import rx.schedulers.TestScheduler
import rx.subjects.PublishSubject
import spock.lang.Specification

import java.util.concurrent.Semaphore

public class RxBleRadioOperationConnectTest extends Specification {

    BluetoothDevice mockBluetoothDevice = Mock BluetoothDevice
    BluetoothGatt mockGatt = Mock BluetoothGatt
    String mockMacAddress = "test"
    RxBleGattCallback mockCallback
    BleConnectionCompat mockBleConnectionCompat
    TestSubscriber<BluetoothGatt> testSubscriber = new TestSubscriber()
    MockOperationTimeoutConfiguration timeoutConfiguration
    PublishSubject<RxBleConnection.RxBleConnectionState> onConnectionStateSubject = PublishSubject.create()
    PublishSubject observeDisconnectPublishSubject = PublishSubject.create()
    Semaphore mockSemaphore = Mock Semaphore
    Subscription asObservableSubscription
    BluetoothGattProvider mockBluetoothGattProvider
    TestScheduler timeoutScheduler
    RxBleRadioOperationConnect objectUnderTest

    def setup() {
        mockBluetoothGattProvider = Mock(BluetoothGattProvider)
        mockCallback = Mock RxBleGattCallback
        mockCallback.getOnConnectionStateChange() >> onConnectionStateSubject
        mockCallback.observeDisconnect() >> observeDisconnectPublishSubject

        timeoutScheduler = new TestScheduler()
        timeoutConfiguration = new MockOperationTimeoutConfiguration(timeoutScheduler)

        mockBleConnectionCompat = Mock(BleConnectionCompat)
        mockBleConnectionCompat.connectGatt(_, _, _) >> mockGatt

        mockGatt.getDevice() >> mockBluetoothDevice
        mockBluetoothDevice.getAddress() >> mockMacAddress

        prepareObjectUnderTest(false)
    }

    def prepareObjectUnderTest(boolean autoConnect) {
        objectUnderTest = new RxBleRadioOperationConnect(mockBluetoothDevice, mockBleConnectionCompat, mockCallback,
                mockBluetoothGattProvider, timeoutConfiguration, autoConnect)
        objectUnderTest.setRadioBlockingSemaphore(mockSemaphore)
        asObservableSubscription = objectUnderTest.asObservable().subscribe(testSubscriber)
    }

    def "asObservable() should not emit onNext before connection is established"() {

        given:
        objectUnderTest.run()

        when:
        emitConnectingConnectionState()

        then:
        testSubscriber.assertNoValues()
    }

    def "asObservable() should emit onNext after connection is established"() {

        given:
        objectUnderTest.run()

        when:
        emitConnectedConnectionState()

        then:
        testSubscriber.assertValueCount(1)
    }

    def "asObservable() should emit onNext with BluetoothGatt after connection is established"() {

        given:
        objectUnderTest.run()

        when:
        emitConnectedConnectionState()

        then:
        testSubscriber.assertAnyOnNext {
            it instanceof BluetoothGatt
        }
    }

    def "should release Semaphore after successful connection"() {

        given:
        objectUnderTest.run()

        when:
        emitConnectedConnectionState()

        then:
        1 * mockSemaphore.release()
    }

    def "should release Semaphore when connection failed"() {

        given:
        objectUnderTest.run()

        when:
        emitConnectionError(new Throwable("test"))

        then:
        1 * mockSemaphore.release()
    }

    def "should release Semaphore when unsubscribed before connection is established"() {

        given:
        objectUnderTest.run()

        when:
        asObservableSubscription.unsubscribe()

        then:
        1 * mockSemaphore.release()
    }

    def "should emit BluetoothGattCallbackTimeoutException with a valid mac address on CallbackTimeout"() {

        given:
        objectUnderTest.run()
        mockBluetoothGattProvider.getBluetoothGatt() >> mockGatt

        when:
        timeoutScheduler.advanceTimeBy(35, TimeUnit.SECONDS)

        then:
        testSubscriber.assertError {
            it instanceof BleGattCallbackTimeoutException && it.getMacAddress() == mockMacAddress
        }
    }

    private emitConnectedConnectionState() {
        mockBluetoothGattProvider.getBluetoothGatt() >> mockGatt
        onConnectionStateSubject.onNext(RxBleConnection.RxBleConnectionState.CONNECTED)
    }

    private emitConnectingConnectionState() {
        onConnectionStateSubject.onNext(RxBleConnection.RxBleConnectionState.CONNECTING)
    }

    private emitConnectionError(Throwable throwable) {
        onConnectionStateSubject.onError(throwable)
    }
}