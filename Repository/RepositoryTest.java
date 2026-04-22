package com.example.do_an_tot_nghiep.Repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.lifecycle.MutableLiveData;

import com.example.do_an_tot_nghiep.Configuration.HTTPRequest;
import com.example.do_an_tot_nghiep.Configuration.HTTPService;
import com.example.do_an_tot_nghiep.Container.AppointmentQueue;
import com.example.do_an_tot_nghiep.Container.AppointmentReadAll;
import com.example.do_an_tot_nghiep.Container.AppointmentReadByID;
import com.example.do_an_tot_nghiep.Container.BookingCreate;
import com.example.do_an_tot_nghiep.Container.BookingPhotoReadAll;
import com.example.do_an_tot_nghiep.Container.BookingReadAll;
import com.example.do_an_tot_nghiep.Container.DoctorReadAll;
import com.example.do_an_tot_nghiep.Container.NotificationReadAll;
import com.example.do_an_tot_nghiep.Container.RecordReadByID;
import com.example.do_an_tot_nghiep.Container.ServiceReadByID;
import com.example.do_an_tot_nghiep.Container.SpecialityReadAll;
import com.example.do_an_tot_nghiep.Container.TreatmentReadByID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockedStatic;

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
 * Unit test suite for repository classes that depend on Retrofit callbacks and LiveData updates.
 *
 * Test strategy:
 * - Mock static HTTPService factory so repositories use mocked Retrofit + HTTPRequest.
 * - Capture Retrofit callback passed to enqueue(), then trigger success/error/failure manually.
 * - Verify repository output LiveData/SingleLiveEvent and animation flags.
 */
public class RepositoryTest {

    @Rule
    public SynchronousTaskExecutorRule instantTaskExecutorRule = new SynchronousTaskExecutorRule();

    private Retrofit retrofit;
    private HTTPRequest api;
    private MockedStatic<HTTPService> httpServiceMock;
    private Map<String, String> headers;
    private Map<String, String> params;

    @Before
    // Build a controlled environment for each test case.
    public void setUp() {
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
    // Close static mock to avoid leaking state between test cases.
    public void tearDown() {
        httpServiceMock.close();
    }

    // Test Case ID: TC-REPO-001
    // Verify Appointment readAll success updates response data and turns off animation.
    @Test
    public void givenAppointmentReadAll_whenApiSuccess_thenUpdateDataAndStopAnimation() {
        AppointmentRepository repositoryUnderTest = new AppointmentRepository();
        Call<AppointmentReadAll> mockApiCall = mockCall();
        AppointmentReadAll mockResponseBody = mock(AppointmentReadAll.class);

        when(api.appointmentReadAll(headers, params)).thenReturn(mockApiCall);
        AtomicReference<Callback<AppointmentReadAll>> capturedCallbackRef = captureCallback(mockApiCall);

        repositoryUnderTest.readAll(headers, params);
        assertEquals(Boolean.TRUE, repositoryUnderTest.getAnimation().getValue());

        capturedCallbackRef.get().onResponse(mockApiCall, Response.success(mockResponseBody));

        assertSame(mockResponseBody, repositoryUnderTest.getReadAllResponse().getValue());
        assertEquals(Boolean.FALSE, repositoryUnderTest.getAnimation().getValue());
    }

    // Test Case ID: TC-REPO-002
    // Verify Appointment readByID error response emits null and turns off animation.
    @Test
    public void givenAppointmentReadById_whenApiError_thenEmitNullAndStopAnimation() {
        AppointmentRepository repositoryUnderTest = new AppointmentRepository();
        Call<AppointmentReadByID> mockApiCall = mockCall();

        when(api.appointmentReadByID(headers, "A1")).thenReturn(mockApiCall);
        AtomicReference<Callback<AppointmentReadByID>> capturedCallbackRef = captureCallback(mockApiCall);

        repositoryUnderTest.readByID(headers, "A1");
        capturedCallbackRef.get().onResponse(mockApiCall, errorResponse());

        assertEquals(null, repositoryUnderTest.getReadByIDResponse().getValue());
        assertEquals(Boolean.FALSE, repositoryUnderTest.getAnimation().getValue());
    }

    // Test Case ID: TC-REPO-003
    // Verify Booking create maps body fields to API call and emits success data.
    @Test
    public void givenBookingCreateRequest_whenApiSuccess_thenMapFieldsAndEmitData() {
        BookingRepository repositoryUnderTest = new BookingRepository();
        Call<BookingCreate> mockApiCall = mockCall();
        BookingCreate mockResponseBody = mock(BookingCreate.class);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("doctorId", "D1");
        requestBody.put("serviceId", "S1");
        requestBody.put("bookingName", "Nguyen Van A");
        requestBody.put("bookingPhone", "0123456789");
        requestBody.put("name", "Patient A");
        requestBody.put("gender", "male");
        requestBody.put("address", "Ha Noi");
        requestBody.put("reason", "Headache");
        requestBody.put("birthday", "2000-01-01");
        requestBody.put("appointmentTime", "08:30");
        requestBody.put("appointmentDate", "2026-04-30");

        when(api.bookingCreate(eq(headers), eq("D1"), eq("S1"), eq("Nguyen Van A"), eq("0123456789"),
                eq("Patient A"), eq("male"), eq("Ha Noi"), eq("Headache"), eq("2000-01-01"),
                eq("08:30"), eq("2026-04-30"))).thenReturn(mockApiCall);

        AtomicReference<Callback<BookingCreate>> capturedCallbackRef = captureCallback(mockApiCall);

        repositoryUnderTest.create(headers, requestBody);
        capturedCallbackRef.get().onResponse(mockApiCall, Response.success(mockResponseBody));

        assertSame(mockResponseBody, repositoryUnderTest.getBookingCreate().getValue());
        assertEquals(Boolean.FALSE, repositoryUnderTest.getAnimation().getValue());
    }

    // Test Case ID: TC-REPO-004
    // Verify Booking readAll failure emits null and turns off animation.
    @Test
    public void givenBookingReadAll_whenApiFailure_thenEmitNullAndStopAnimation() {
        BookingRepository repositoryUnderTest = new BookingRepository();
        Call<BookingReadAll> mockApiCall = mockCall();

        when(api.bookingReadAll(headers, params)).thenReturn(mockApiCall);
        AtomicReference<Callback<BookingReadAll>> capturedCallbackRef = captureCallback(mockApiCall);

        repositoryUnderTest.readAll(headers, params);
        capturedCallbackRef.get().onFailure(mockApiCall, new RuntimeException("network"));

        assertEquals(null, repositoryUnderTest.getReadAllResponse().getValue());
        assertEquals(Boolean.FALSE, repositoryUnderTest.getAnimation().getValue());
    }

    // Test Case ID: TC-REPO-005
    // Verify BookingPhoto readAll error emits null and turns off animation.
    @Test
    public void givenBookingPhotoReadAll_whenApiError_thenEmitNullAndStopAnimation() {
        BookingPhotoRepository repositoryUnderTest = new BookingPhotoRepository();
        Call<BookingPhotoReadAll> mockApiCall = mockCall();

        when(api.bookingPhotoReadAll(headers, "B1")).thenReturn(mockApiCall);
        AtomicReference<Callback<BookingPhotoReadAll>> capturedCallbackRef = captureCallback(mockApiCall);

        repositoryUnderTest.readAll(headers, "B1");
        capturedCallbackRef.get().onResponse(mockApiCall, errorResponse());

        assertEquals(null, repositoryUnderTest.getReadAllResponse().getValue());
        assertEquals(Boolean.FALSE, repositoryUnderTest.getAnimation().getValue());
    }

    // Test Case ID: TC-REPO-006
    // Verify Doctor readAll success emits data and turns off animation.
    @Test
    public void givenDoctorReadAll_whenApiSuccess_thenEmitDataAndStopAnimation() {
        DoctorRepository repositoryUnderTest = new DoctorRepository();
        Call<DoctorReadAll> mockApiCall = mockCall();
        DoctorReadAll mockResponseBody = mock(DoctorReadAll.class);

        when(api.doctorReadAll(headers, params)).thenReturn(mockApiCall);
        AtomicReference<Callback<DoctorReadAll>> capturedCallbackRef = captureCallback(mockApiCall);

        repositoryUnderTest.readAll(headers, params);
        capturedCallbackRef.get().onResponse(mockApiCall, Response.success(mockResponseBody));

        assertSame(mockResponseBody, repositoryUnderTest.getReadAllResponse().getValue());
        assertEquals(Boolean.FALSE, repositoryUnderTest.getAnimation().getValue());
    }

    // Test Case ID: TC-REPO-007
    // Verify Service readByID failure emits null and turns off animation.
    @Test
    public void givenServiceReadById_whenApiFailure_thenEmitNullAndStopAnimation() {
        ServiceRepository repositoryUnderTest = new ServiceRepository();
        Call<ServiceReadByID> mockApiCall = mockCall();

        when(api.serviceReadByID(headers, "SV1")).thenReturn(mockApiCall);
        AtomicReference<Callback<ServiceReadByID>> capturedCallbackRef = captureCallback(mockApiCall);

        repositoryUnderTest.readByID(headers, "SV1");
        capturedCallbackRef.get().onFailure(mockApiCall, new RuntimeException("timeout"));

        assertEquals(null, repositoryUnderTest.getReadByIDResponse().getValue());
        assertEquals(Boolean.FALSE, repositoryUnderTest.getAnimation().getValue());
    }

    // Test Case ID: TC-REPO-008
    // Verify Speciality readAll error emits null and turns off animation.
    @Test
    public void givenSpecialityReadAll_whenApiError_thenEmitNullAndStopAnimation() {
        SpecialityRepository repositoryUnderTest = new SpecialityRepository();
        Call<SpecialityReadAll> mockApiCall = mockCall();

        when(api.specialityReadAll(headers, params)).thenReturn(mockApiCall);
        AtomicReference<Callback<SpecialityReadAll>> capturedCallbackRef = captureCallback(mockApiCall);

        repositoryUnderTest.readAll(headers, params);
        capturedCallbackRef.get().onResponse(mockApiCall, errorResponse());

        assertEquals(null, repositoryUnderTest.getReadAllResponse().getValue());
        assertEquals(Boolean.FALSE, repositoryUnderTest.getAnimation().getValue());
    }

    // Test Case ID: TC-REPO-009
    // Verify Treatment readByID success emits data and turns off animation.
    @Test
    public void givenTreatmentReadById_whenApiSuccess_thenEmitDataAndStopAnimation() {
        TreatmentRepository repositoryUnderTest = new TreatmentRepository();
        Call<TreatmentReadByID> mockApiCall = mockCall();
        TreatmentReadByID mockResponseBody = mock(TreatmentReadByID.class);

        when(api.treatmentReadByID(headers, "T1")).thenReturn(mockApiCall);
        AtomicReference<Callback<TreatmentReadByID>> capturedCallbackRef = captureCallback(mockApiCall);

        repositoryUnderTest.readByID(headers, "T1");
        capturedCallbackRef.get().onResponse(mockApiCall, Response.success(mockResponseBody));

        assertSame(mockResponseBody, repositoryUnderTest.getReadByIDResponse().getValue());
        assertEquals(Boolean.FALSE, repositoryUnderTest.getAnimation().getValue());
    }

    // Test Case ID: TC-REPO-010
    // Verify AppointmentQueue keeps old value when second request fails.
    @Test
    public void givenAppointmentQueueWithExistingData_whenNextCallFails_thenKeepOldData() {
        AppointmentQueueRepository repositoryUnderTest = new AppointmentQueueRepository();
        Call<AppointmentQueue> mockSuccessCall = mockCall();
        Call<AppointmentQueue> mockFailureCall = mockCall();
        AppointmentQueue mockFirstResponseBody = mock(AppointmentQueue.class);

        when(api.appointmentQueue(headers, params)).thenReturn(mockSuccessCall).thenReturn(mockFailureCall);
        AtomicReference<Callback<AppointmentQueue>> capturedSuccessCallbackRef = captureCallback(mockSuccessCall);
        AtomicReference<Callback<AppointmentQueue>> capturedFailureCallbackRef = captureCallback(mockFailureCall);

        MutableLiveData<AppointmentQueue> firstLiveDataRef = repositoryUnderTest.getAppointmentQueue(headers, params);
        capturedSuccessCallbackRef.get().onResponse(mockSuccessCall, Response.success(mockFirstResponseBody));
        assertSame(mockFirstResponseBody, firstLiveDataRef.getValue());

        MutableLiveData<AppointmentQueue> secondLiveDataRef = repositoryUnderTest.getAppointmentQueue(headers, params);
        capturedFailureCallbackRef.get().onFailure(mockFailureCall, new RuntimeException("network down"));

        assertSame(firstLiveDataRef, secondLiveDataRef);
        assertSame(mockFirstResponseBody, secondLiveDataRef.getValue());
    }

    // Test Case ID: TC-REPO-011
    // Verify Notification readAll keeps old value when subsequent call fails.
    @Test
    public void givenNotificationDataLoaded_whenNextCallFails_thenKeepOldDataAndStopAnimation() {
        NotificationRepository repositoryUnderTest = new NotificationRepository();
        Call<NotificationReadAll> mockSuccessCall = mockCall();
        Call<NotificationReadAll> mockFailureCall = mockCall();
        NotificationReadAll mockFirstResponseBody = mock(NotificationReadAll.class);

        when(api.notificationReadAll(headers)).thenReturn(mockSuccessCall).thenReturn(mockFailureCall);
        AtomicReference<Callback<NotificationReadAll>> capturedSuccessCallbackRef = captureCallback(mockSuccessCall);
        AtomicReference<Callback<NotificationReadAll>> capturedFailureCallbackRef = captureCallback(mockFailureCall);

        MutableLiveData<NotificationReadAll> notificationLiveData = repositoryUnderTest.readAll(headers);
        capturedSuccessCallbackRef.get().onResponse(mockSuccessCall, Response.success(mockFirstResponseBody));
        assertSame(mockFirstResponseBody, notificationLiveData.getValue());

        repositoryUnderTest.readAll(headers);
        capturedFailureCallbackRef.get().onFailure(mockFailureCall, new RuntimeException("offline"));

        assertSame(mockFirstResponseBody, notificationLiveData.getValue());
        assertEquals(Boolean.FALSE, repositoryUnderTest.getAnimation().getValue());
    }

    // Test Case ID: TC-REPO-012
    // Verify Record readByID keeps old value when subsequent call fails.
    @Test
    public void givenRecordDataLoaded_whenNextCallFails_thenKeepOldDataAndStopAnimation() {
        RecordRepository repositoryUnderTest = new RecordRepository();
        Call<RecordReadByID> mockSuccessCall = mockCall();
        Call<RecordReadByID> mockFailureCall = mockCall();
        RecordReadByID mockFirstResponseBody = mock(RecordReadByID.class);

        when(api.recordReadById(headers, "R1")).thenReturn(mockSuccessCall).thenReturn(mockFailureCall);
        AtomicReference<Callback<RecordReadByID>> capturedSuccessCallbackRef = captureCallback(mockSuccessCall);
        AtomicReference<Callback<RecordReadByID>> capturedFailureCallbackRef = captureCallback(mockFailureCall);

        MutableLiveData<RecordReadByID> recordLiveData = repositoryUnderTest.readByID(headers, "R1");
        capturedSuccessCallbackRef.get().onResponse(mockSuccessCall, Response.success(mockFirstResponseBody));
        assertSame(mockFirstResponseBody, recordLiveData.getValue());

        repositoryUnderTest.readByID(headers, "R1");
        capturedFailureCallbackRef.get().onFailure(mockFailureCall, new RuntimeException("no internet"));

        assertSame(mockFirstResponseBody, recordLiveData.getValue());
        assertEquals(Boolean.FALSE, repositoryUnderTest.getAnimation().getValue());
    }


    // Captures callback instance passed into Retrofit enqueue() so tests can trigger it manually.
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

    // Creates a generic HTTP 400 Retrofit response for error-path testing.
    private <T> Response<T> errorResponse() {
        return Response.error(
                400,
                ResponseBody.create(
                        MediaType.parse("application/json"),
                        "{\"message\":\"error\"}"
                )
        );
    }
}


