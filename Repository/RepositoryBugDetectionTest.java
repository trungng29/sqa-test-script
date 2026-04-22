package com.example.do_an_tot_nghiep.Repository;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.lifecycle.MutableLiveData;

import com.example.do_an_tot_nghiep.Configuration.HTTPRequest;
import com.example.do_an_tot_nghiep.Configuration.HTTPService;
import com.example.do_an_tot_nghiep.Container.AppointmentQueue;
import com.example.do_an_tot_nghiep.Container.BookingCreate;
import com.example.do_an_tot_nghiep.Container.NotificationReadAll;
import com.example.do_an_tot_nghiep.Container.RecordReadByID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Bug-focused unit tests for repository implementations.
 *
 * This class intentionally mixes pass/fail cases:
 * - Pass cases validate current happy-path and known handled error-path behavior.
 * - Fail cases expose real logic gaps (stale data, missing resets, missing validation).
 */
public class RepositoryBugDetectionTest {

    @Rule
    public SynchronousTaskExecutorRule synchronousTaskExecutorRule = new SynchronousTaskExecutorRule();

    private AutoCloseable mocks;
    private Retrofit retrofit;
    private HTTPRequest api;
    private MockedStatic<HTTPService> httpServiceMock;

    private Map<String, String> headers;
    private Map<String, String> params;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        retrofit = mock(Retrofit.class);
        api = mock(HTTPRequest.class);
        when(retrofit.create(HTTPRequest.class)).thenReturn(api);

        httpServiceMock = org.mockito.Mockito.mockStatic(HTTPService.class);
        httpServiceMock.when(HTTPService::getInstance).thenReturn(retrofit);

        headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");
        headers.put("Type", "patient");

        params = new HashMap<>();
        params.put("page", "1");
        params.put("limit", "10");
    }

    @After
    public void tearDown() throws Exception {
        httpServiceMock.close();
        mocks.close();
    }

    // ---------- PASS TESTS (valid/handled partitions) ----------

    // EP-VALID: success response contains body and updates LiveData.
    @Test
    public void notification_success_shouldUpdateDataAndStopAnimation() {
        NotificationRepository repositoryUnderTest = new NotificationRepository();
        Call<NotificationReadAll> call = mockCall();
        NotificationReadAll body = mock(NotificationReadAll.class);

        when(api.notificationReadAll(headers)).thenReturn(call);
        AtomicReference<Callback<NotificationReadAll>> callbackRef = captureCallback(call);

        MutableLiveData<NotificationReadAll> liveData = repositoryUnderTest.readAll(headers);
        verify(api).notificationReadAll(headers);

        callbackRef.get().onResponse(call, Response.success(body));

        assertSame(body, liveData.getValue());
        assertFalse(Boolean.TRUE.equals(repositoryUnderTest.getAnimation().getValue()));
    }

    // EP-INVALID (handled): errorBody present sets null and stops animation.
    @Test
    public void notification_errorBodyPresent_shouldClearDataAndStopAnimation() {
        NotificationRepository repositoryUnderTest = new NotificationRepository();
        Call<NotificationReadAll> call = mockCall();

        when(api.notificationReadAll(headers)).thenReturn(call);
        AtomicReference<Callback<NotificationReadAll>> callbackRef = captureCallback(call);

        repositoryUnderTest.readAll(headers);
        callbackRef.get().onResponse(call, errorResponse());

        assertNull(repositoryUnderTest.getReadAllResponse().getValue());
        assertFalse(Boolean.TRUE.equals(repositoryUnderTest.getAnimation().getValue()));
    }

    // EP-VALID: Record success path updates value and stops animation.
    @Test
    public void record_success_shouldUpdateDataAndStopAnimation() {
        RecordRepository repositoryUnderTest = new RecordRepository();
        Call<RecordReadByID> call = mockCall();
        RecordReadByID body = mock(RecordReadByID.class);

        when(api.recordReadById(headers, "R1")).thenReturn(call);
        AtomicReference<Callback<RecordReadByID>> callbackRef = captureCallback(call);

        MutableLiveData<RecordReadByID> liveData = repositoryUnderTest.readByID(headers, "R1");
        callbackRef.get().onResponse(call, Response.success(body));

        assertSame(body, liveData.getValue());
        assertFalse(Boolean.TRUE.equals(repositoryUnderTest.getAnimation().getValue()));
    }

    // ---------- FAIL TESTS (real bugs / edge gaps) ----------

    // Bug: onFailure() in NotificationRepository does not clear stale data.
    @Test
    public void notification_onFailure_shouldClearStaleData_butCurrentlyDoesNot() {
        NotificationRepository repositoryUnderTest = new NotificationRepository();
        Call<NotificationReadAll> successCall = mockCall();
        Call<NotificationReadAll> failureCall = mockCall();
        NotificationReadAll cached = mock(NotificationReadAll.class);

        when(api.notificationReadAll(headers)).thenReturn(successCall).thenReturn(failureCall);
        AtomicReference<Callback<NotificationReadAll>> successCallbackRef = captureCallback(successCall);
        AtomicReference<Callback<NotificationReadAll>> failureCallbackRef = captureCallback(failureCall);

        MutableLiveData<NotificationReadAll> liveData = repositoryUnderTest.readAll(headers);
        successCallbackRef.get().onResponse(successCall, Response.success(cached));

        repositoryUnderTest.readAll(headers);
        failureCallbackRef.get().onFailure(failureCall, new RuntimeException("offline"));

        // Expected safer behavior: clear stale value on failure.
        assertNull(liveData.getValue());
    }

    // Bug: unsuccessful response with null errorBody leaves animation/data untouched in NotificationRepository.
    @Test
    public void notification_unsuccessfulWithoutErrorBody_shouldResetState_butCurrentlyDoesNot() {
        NotificationRepository repositoryUnderTest = new NotificationRepository();
        Call<NotificationReadAll> successCall = mockCall();
        Call<NotificationReadAll> errorCall = mockCall();
        NotificationReadAll cached = mock(NotificationReadAll.class);

        when(api.notificationReadAll(headers)).thenReturn(successCall).thenReturn(errorCall);
        AtomicReference<Callback<NotificationReadAll>> successCallbackRef = captureCallback(successCall);
        AtomicReference<Callback<NotificationReadAll>> errorCallbackRef = captureCallback(errorCall);

        MutableLiveData<NotificationReadAll> liveData = repositoryUnderTest.readAll(headers);
        successCallbackRef.get().onResponse(successCall, Response.success(cached));

        @SuppressWarnings("unchecked")
        Response<NotificationReadAll> unsuccessful = mock(Response.class);
        when(unsuccessful.isSuccessful()).thenReturn(false);
        when(unsuccessful.errorBody()).thenReturn(null);

        repositoryUnderTest.readAll(headers);
        errorCallbackRef.get().onResponse(errorCall, unsuccessful);

        assertFalse(Boolean.TRUE.equals(repositoryUnderTest.getAnimation().getValue()));
        assertNull(liveData.getValue());
    }

    // Bug: onFailure() in RecordRepository does not clear stale data.
    @Test
    public void record_onFailure_shouldClearStaleData_butCurrentlyDoesNot() {
        RecordRepository repositoryUnderTest = new RecordRepository();
        Call<RecordReadByID> successCall = mockCall();
        Call<RecordReadByID> failureCall = mockCall();
        RecordReadByID cached = mock(RecordReadByID.class);

        when(api.recordReadById(headers, "R1")).thenReturn(successCall).thenReturn(failureCall);
        AtomicReference<Callback<RecordReadByID>> successCallbackRef = captureCallback(successCall);
        AtomicReference<Callback<RecordReadByID>> failureCallbackRef = captureCallback(failureCall);

        MutableLiveData<RecordReadByID> liveData = repositoryUnderTest.readByID(headers, "R1");
        successCallbackRef.get().onResponse(successCall, Response.success(cached));

        repositoryUnderTest.readByID(headers, "R1");
        failureCallbackRef.get().onFailure(failureCall, new RuntimeException("offline"));

        assertNull(liveData.getValue());
    }

    // Bug: onFailure() in AppointmentQueueRepository keeps stale queue data.
    @Test
    public void appointmentQueue_onFailure_shouldClearStaleData_butCurrentlyDoesNot() {
        AppointmentQueueRepository repositoryUnderTest = new AppointmentQueueRepository();
        Call<AppointmentQueue> successCall = mockCall();
        Call<AppointmentQueue> failureCall = mockCall();
        AppointmentQueue cached = mock(AppointmentQueue.class);

        when(api.appointmentQueue(headers, params)).thenReturn(successCall).thenReturn(failureCall);
        AtomicReference<Callback<AppointmentQueue>> successCallbackRef = captureCallback(successCall);
        AtomicReference<Callback<AppointmentQueue>> failureCallbackRef = captureCallback(failureCall);

        MutableLiveData<AppointmentQueue> liveData = repositoryUnderTest.getAppointmentQueue(headers, params);
        successCallbackRef.get().onResponse(successCall, Response.success(cached));

        repositoryUnderTest.getAppointmentQueue(headers, params);
        failureCallbackRef.get().onFailure(failureCall, new RuntimeException("offline"));

        assertNull(liveData.getValue());
    }

    // BVA-null: missing request body in BookingRepository.create should be validated, but currently throws NPE.
    @Test
    public void bookingCreate_nullBody_shouldBeHandledSafely_butCurrentlyCrashes() {
        BookingRepository repositoryUnderTest = new BookingRepository();

        // Expected behavior: no crash, emit null and stop animation.
        repositoryUnderTest.create(headers, null);

        assertNull(repositoryUnderTest.getBookingCreate().getValue());
        assertFalse(Boolean.TRUE.equals(repositoryUnderTest.getAnimation().getValue()));
    }

    private <T> AtomicReference<Callback<T>> captureCallback(Call<T> call) {
        AtomicReference<Callback<T>> callbackRef = new AtomicReference<>();
        doAnswer(invocation -> {
            callbackRef.set(invocation.getArgument(0));
            return null;
        }).when(call).enqueue(any());
        return callbackRef;
    }

    @SuppressWarnings("unchecked")
    private <T> Call<T> mockCall() {
        return (Call<T>) mock(Call.class);
    }

    private <T> Response<T> errorResponse() {
        return Response.error(
                400,
                ResponseBody.create(MediaType.parse("application/json"), "{\"message\":\"error\"}")
        );
    }
}

