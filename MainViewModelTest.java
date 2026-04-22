package com.example.do_an_tot_nghiep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import androidx.lifecycle.MutableLiveData;

import com.example.do_an_tot_nghiep.Configuration.HTTPRequest;
import com.example.do_an_tot_nghiep.Configuration.HTTPService;
import com.example.do_an_tot_nghiep.Container.PatientProfile;
import com.example.do_an_tot_nghiep.Repository.SynchronousTaskExecutorRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Unit tests for MainViewModel (closest business-logic layer in this workspace).
 * No explicit UseCase classes were found, so this test covers the dependency-interaction logic
 * that behaves like a use-case: request orchestration, callback handling, and LiveData updates.
 */
public class MainViewModelTest {

    @Rule
    public SynchronousTaskExecutorRule synchronousTaskExecutorRule = new SynchronousTaskExecutorRule();

    private AutoCloseable mocks;

    @Mock
    private Retrofit retrofit;

    @Mock
    private HTTPRequest api;

    private MockedStatic<HTTPService> httpServiceMock;
    private MainViewModel viewModelUnderTest;
    private Map<String, String> headers;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        viewModelUnderTest = new MainViewModel();

        doReturn(api).when(retrofit).create(HTTPRequest.class);

        httpServiceMock = org.mockito.Mockito.mockStatic(HTTPService.class);
        httpServiceMock.when(HTTPService::getInstance).thenReturn(retrofit);

        headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");
        headers.put("Type", "patient");
    }

    @After
    public void tearDown() throws Exception {
        httpServiceMock.close();
        mocks.close();
    }

    // Test Case ID: TC-USECASE-001
    // Valid input: successful response should update LiveData with profile data.
    @Test
    public void givenValidHeadersWhenReadPersonalInformationSuccessThenLiveDataContainsData() {
        Call<PatientProfile> mockApiCall = mock(Call.class);
        PatientProfile expectedProfile = mock(PatientProfile.class);

        doReturn(mockApiCall).when(api).readPersonalInformation(headers);
        AtomicReference<Callback<PatientProfile>> capturedCallbackRef = captureCallback(mockApiCall);

        viewModelUnderTest.readPersonalInformation(headers);

        verify(api).readPersonalInformation(headers);
        verify(mockApiCall).enqueue(any());

        capturedCallbackRef.get().onResponse(mockApiCall, Response.success(expectedProfile));

        assertSame(expectedProfile, viewModelUnderTest.getResponse().getValue());
    }

    // Test Case ID: TC-USECASE-002
    // Invalid input partition: error response with errorBody should clear LiveData.
    @Test
    public void givenErrorResponseWhenReadPersonalInformationThenLiveDataBecomesNull() {
        Call<PatientProfile> mockApiCall = mock(Call.class);

        doReturn(mockApiCall).when(api).readPersonalInformation(headers);
        AtomicReference<Callback<PatientProfile>> capturedCallbackRef = captureCallback(mockApiCall);

        viewModelUnderTest.readPersonalInformation(headers);
        capturedCallbackRef.get().onResponse(
                mockApiCall,
                Response.error(400, okhttp3.ResponseBody.create(okhttp3.MediaType.parse("application/json"), "{\"message\":\"bad request\"}"))
        );

        assertNull(viewModelUnderTest.getResponse().getValue());
    }

    // Test Case ID: TC-USECASE-003
    // Boundary value: a success response with null body should not crash and should leave LiveData null.
    @Test
    public void givenSuccessResponseWithNullBodyWhenReadPersonalInformationThenLiveDataStaysNull() {
        Call<PatientProfile> mockApiCall = mock(Call.class);

        doReturn(mockApiCall).when(api).readPersonalInformation(headers);
        AtomicReference<Callback<PatientProfile>> capturedCallbackRef = captureCallback(mockApiCall);

        viewModelUnderTest.readPersonalInformation(headers);
        capturedCallbackRef.get().onResponse(mockApiCall, Response.success((PatientProfile) null));

        assertNull(viewModelUnderTest.getResponse().getValue());
    }

    // Test Case ID: TC-USECASE-004
    // Real bug: null headers should be validated before calling the API, but current code still calls it.
    @Test
    public void givenNullHeadersWhenReadPersonalInformationThenShouldNotCallApi() {
        Call<PatientProfile> mockApiCall = mock(Call.class);

        doReturn(mockApiCall).when(api).readPersonalInformation(isNull());

        viewModelUnderTest.readPersonalInformation(null);

        // This fails because MainViewModel does not validate null headers before invoking the API.
        verify(api, never()).readPersonalInformation(isNull());
    }

    // Test Case ID: TC-USECASE-005
    // Real bug: if a second response has no errorBody, the stale profile remains in LiveData.
    @Test
    public void givenCachedProfileWhenNextErrorHasNoBodyThenShouldClearStaleData() {
        Call<PatientProfile> successCall = mock(Call.class);
        Call<PatientProfile> failureCall = mock(Call.class);
        PatientProfile cachedProfile = mock(PatientProfile.class);

        doReturn(successCall, failureCall).when(api).readPersonalInformation(headers);
        AtomicReference<Callback<PatientProfile>> successCallbackRef = captureCallback(successCall);
        AtomicReference<Callback<PatientProfile>> failureCallbackRef = captureCallback(failureCall);

        viewModelUnderTest.readPersonalInformation(headers);
        successCallbackRef.get().onResponse(successCall, Response.success(cachedProfile));
        assertSame(cachedProfile, viewModelUnderTest.getResponse().getValue());

        viewModelUnderTest.readPersonalInformation(headers);
        @SuppressWarnings("unchecked")
        Response<PatientProfile> unsuccessfulResponse = mock(Response.class);
        doReturn(false).when(unsuccessfulResponse).isSuccessful();
        doReturn(null).when(unsuccessfulResponse).errorBody();
        failureCallbackRef.get().onResponse(failureCall, unsuccessfulResponse);

        // This fails because current code ignores unsuccessful responses when errorBody() is null,
        // leaving the previous profile in LiveData.
        assertNull(viewModelUnderTest.getResponse().getValue());
    }

    // Test Case ID: TC-USECASE-006
    // Failure callback should clear LiveData; this is the current expected behavior and passes.
    @Test
    public void givenFailureCallbackWhenReadPersonalInformationThenLiveDataBecomesNull() {
        Call<PatientProfile> mockApiCall = mock(Call.class);

        doReturn(mockApiCall).when(api).readPersonalInformation(headers);
        AtomicReference<Callback<PatientProfile>> capturedCallbackRef = captureCallback(mockApiCall);

        viewModelUnderTest.readPersonalInformation(headers);
        capturedCallbackRef.get().onFailure(mockApiCall, new RuntimeException("network down"));

        assertNull(viewModelUnderTest.getResponse().getValue());
    }

    private AtomicReference<Callback<PatientProfile>> captureCallback(Call<PatientProfile> call) {
        AtomicReference<Callback<PatientProfile>> callbackRef = new AtomicReference<>();
        doAnswer(invocation -> {
            callbackRef.set(invocation.getArgument(0));
            return null;
        }).when(call).enqueue(any());
        return callbackRef;
    }
}
