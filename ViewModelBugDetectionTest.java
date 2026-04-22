package com.example.do_an_tot_nghiep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.do_an_tot_nghiep.Appointmentpage.AppointmentpageViewModel;
import com.example.do_an_tot_nghiep.Bookingpage.BookingpageViewModel;
import com.example.do_an_tot_nghiep.Configuration.HTTPRequest;
import com.example.do_an_tot_nghiep.Configuration.HTTPService;
import com.example.do_an_tot_nghiep.Container.AppointmentQueue;
import com.example.do_an_tot_nghiep.Container.AppointmentReadAll;
import com.example.do_an_tot_nghiep.Container.BookingReadByID;
import com.example.do_an_tot_nghiep.Container.DoctorReadAll;
import com.example.do_an_tot_nghiep.Container.Login;
import com.example.do_an_tot_nghiep.Container.NotificationReadAll;
import com.example.do_an_tot_nghiep.Container.PatientProfile;
import com.example.do_an_tot_nghiep.Container.ServiceReadAll;
import com.example.do_an_tot_nghiep.Helper.SingleLiveEvent;
import com.example.do_an_tot_nghiep.Homepage.HomepageViewModel;
import com.example.do_an_tot_nghiep.Loginpage.LoginViewModel;
import com.example.do_an_tot_nghiep.Notificationpage.NotificationViewModel;
import com.example.do_an_tot_nghiep.Repository.AppointmentQueueRepository;
import com.example.do_an_tot_nghiep.Repository.AppointmentRepository;
import com.example.do_an_tot_nghiep.Repository.BookingRepository;
import com.example.do_an_tot_nghiep.Repository.DoctorRepository;
import com.example.do_an_tot_nghiep.Repository.NotificationRepository;
import com.example.do_an_tot_nghiep.Repository.ServiceRepository;
import com.example.do_an_tot_nghiep.Repository.SpecialityRepository;
import com.example.do_an_tot_nghiep.Repository.SynchronousTaskExecutorRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Unit tests for ViewModel classes.
 *
 * Coverage:
 * - LoginViewModel (direct Retrofit orchestration)
 * - NotificationViewModel, BookingpageViewModel, AppointmentpageViewModel, HomepageViewModel
 *   (repository-backed ViewModels)
 *
 * Some tests are intentionally failing because they expose real bugs:
 * - missing lazy initialization / null guards
 * - stale LiveData not cleared on error-body-less failures
 */
public class ViewModelBugDetectionTest {

    @Rule
    public SynchronousTaskExecutorRule synchronousTaskExecutorRule = new SynchronousTaskExecutorRule();

    private AutoCloseable mocks;

    @Mock
    private Retrofit retrofit;

    @Mock
    private HTTPRequest api;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentQueueRepository appointmentQueueRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private SpecialityRepository specialityRepository;

    private MockedStatic<HTTPService> httpServiceMock;
    private Map<String, String> headers;
    private Map<String, String> parameters;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        when(retrofit.create(HTTPRequest.class)).thenReturn(api);
        httpServiceMock = org.mockito.Mockito.mockStatic(HTTPService.class);
        httpServiceMock.when(HTTPService::getInstance).thenReturn(retrofit);

        headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");
        headers.put("Type", "patient");

        parameters = new HashMap<>();
        parameters.put("page", "1");
        parameters.put("limit", "10");
    }

    @After
    public void tearDown() throws Exception {
        httpServiceMock.close();
        mocks.close();
    }

    // Test Case ID: TC-VM-001
    // Valid input: login with phone should update LiveData and stop animation.
    @Test
    public void givenValidPhoneAndPasswordWhenLoginWithPhoneSuccessThenLiveDataContainsLoginAndAnimationStops() {
        LoginViewModel viewModelUnderTest = new LoginViewModel();
        MutableLiveData<Boolean> animationLiveData = viewModelUnderTest.getAnimation();
        MutableLiveData<Login> loginLiveData = viewModelUnderTest.getLoginWithPhoneResponse();

        Call<Login> mockApiCall = mockCall();
        Login expectedLogin = mock(Login.class);

        doReturn(mockApiCall).when(api).login("0123456789", "secret", "patient");
        AtomicReference<Callback<Login>> callbackRef = captureCallback(mockApiCall);

        viewModelUnderTest.loginWithPhone("0123456789", "secret");
        assertEquals(Boolean.TRUE, animationLiveData.getValue());
        verify(api).login("0123456789", "secret", "patient");
        verify(mockApiCall).enqueue(any());

        callbackRef.get().onResponse(mockApiCall, Response.success(expectedLogin));

        assertSame(expectedLogin, loginLiveData.getValue());
        assertEquals(Boolean.FALSE, animationLiveData.getValue());
    }

    // Test Case ID: TC-VM-002
    // Invalid input partition: login failure should clear LiveData and stop animation.
    @Test
    public void givenLoginFailureWhenLoginWithPhoneThenLiveDataBecomesNullAndAnimationStops() {
        LoginViewModel viewModelUnderTest = new LoginViewModel();
        MutableLiveData<Boolean> animationLiveData = viewModelUnderTest.getAnimation();
        MutableLiveData<Login> loginLiveData = viewModelUnderTest.getLoginWithPhoneResponse();

        Call<Login> mockApiCall = mockCall();
        doReturn(mockApiCall).when(api).login("0123456789", "secret", "patient");
        AtomicReference<Callback<Login>> callbackRef = captureCallback(mockApiCall);

        viewModelUnderTest.loginWithPhone("0123456789", "secret");
        callbackRef.get().onFailure(mockApiCall, new RuntimeException("network down"));

        assertNull(loginLiveData.getValue());
        assertEquals(Boolean.FALSE, animationLiveData.getValue());
    }

    // Test Case ID: TC-VM-003
    // Real bug: a fresh LoginViewModel crashes because animation/login LiveData are lazily initialized
    // but loginWithPhone() uses them directly without null guards.
    @Test
    public void givenFreshLoginViewModelWhenLoginWithPhoneCalledThenShouldNotCrash() {
        LoginViewModel viewModelUnderTest = new LoginViewModel();
        Call<Login> mockApiCall = mockCall();
        doReturn(mockApiCall).when(api).login("0123456789", "secret", "patient");

        // This fails with NullPointerException on current code because getAnimation()/getLoginWithPhoneResponse()
        // are not called before loginWithPhone() uses the fields.
        viewModelUnderTest.loginWithPhone("0123456789", "secret");
    }

    // Test Case ID: TC-VM-004
    // Real bug: loginWithGoogle leaves stale data/animation when errorBody() is null.
    @Test
    public void givenCachedGoogleLoginWhenNextErrorHasNoBodyThenShouldClearStaleDataAndStopAnimation() {
        LoginViewModel viewModelUnderTest = new LoginViewModel();
        MutableLiveData<Boolean> animationLiveData = viewModelUnderTest.getAnimation();
        MutableLiveData<Login> loginLiveData = viewModelUnderTest.getLoginWithGoogleResponse();

        Call<Login> successCall = mockCall();
        Call<Login> errorCall = mockCall();
        Login cachedLogin = mock(Login.class);

        doReturn(successCall, errorCall).when(api).loginWithGoogle("patient@test.com", "secret", "patient");
        AtomicReference<Callback<Login>> successCallbackRef = captureCallback(successCall);
        AtomicReference<Callback<Login>> errorCallbackRef = captureCallback(errorCall);

        viewModelUnderTest.loginWithGoogle("patient@test.com", "secret");
        successCallbackRef.get().onResponse(successCall, Response.success(cachedLogin));
        assertSame(cachedLogin, loginLiveData.getValue());

        viewModelUnderTest.loginWithGoogle("patient@test.com", "secret");
        @SuppressWarnings("unchecked")
        Response<Login> unsuccessfulResponse = mock(Response.class);
        doReturn(false).when(unsuccessfulResponse).isSuccessful();
        doReturn(null).when(unsuccessfulResponse).errorBody();
        errorCallbackRef.get().onResponse(errorCall, unsuccessfulResponse);

        // This fails on current code because loginWithGoogle() only clears state when errorBody() != null.
        assertNull(loginLiveData.getValue());
        assertEquals(Boolean.FALSE, animationLiveData.getValue());
    }

    // Test Case ID: TC-VM-005
    // Valid repository interaction: NotificationViewModel should expose repository LiveData values.
    @Test
    public void givenInjectedNotificationRepositoryWhenReadAllSuccessThenViewModelExposesRepositoryLiveData() throws Exception {
        NotificationViewModel viewModelUnderTest = new NotificationViewModel();
        MutableLiveData<NotificationReadAll> repositoryLiveData = new MutableLiveData<>();
        MutableLiveData<Boolean> repositoryAnimation = new MutableLiveData<>();
        NotificationReadAll expectedResponse = mock(NotificationReadAll.class);

        doReturn(repositoryAnimation).when(notificationRepository).getAnimation();
        doReturn(repositoryLiveData).when(notificationRepository).readAll(headers);
        setPrivateField(viewModelUnderTest, "repository", notificationRepository);

        viewModelUnderTest.readAll(headers);
        AtomicReference<NotificationReadAll> observedValue = observe(viewModelUnderTest.getReadAllResponse());
        repositoryLiveData.setValue(expectedResponse);

        assertSame(repositoryLiveData, viewModelUnderTest.getReadAllResponse());
        assertSame(expectedResponse, observedValue.get());
    }

    // Test Case ID: TC-VM-006
    // Real bug: NotificationViewModel.readAll() crashes when instantiate() was not called.
    @Test
    public void givenFreshNotificationViewModelWhenReadAllCalledThenShouldNotCrash() {
        NotificationViewModel viewModelUnderTest = new NotificationViewModel();

        // This fails because repository is never initialized unless instantiate() is called first.
        viewModelUnderTest.readAll(headers);
    }

    // Test Case ID: TC-VM-007
    // Valid repository interaction: BookingpageViewModel should expose repository LiveData values.
    @Test
    public void givenInjectedBookingRepositoriesWhenServiceReadByIdSuccessThenViewModelExposesRepositoryLiveData() throws Exception {
        BookingpageViewModel viewModelUnderTest = new BookingpageViewModel();
        MutableLiveData<Boolean> repositoryAnimation = new MutableLiveData<>();
        SingleLiveEvent<com.example.do_an_tot_nghiep.Container.ServiceReadByID> serviceReadByIdLiveData = new SingleLiveEvent<>();

        doReturn(repositoryAnimation).when(serviceRepository).getAnimation();
        doReturn(serviceReadByIdLiveData).when(serviceRepository).getReadByIDResponse();
        setPrivateField(viewModelUnderTest, "serviceRepository", serviceRepository);

        viewModelUnderTest.serviceReadById(headers, "S1");
        AtomicReference<com.example.do_an_tot_nghiep.Container.ServiceReadByID> observedValue = observe(viewModelUnderTest.getServiceReadByIdResponse());
        verify(serviceRepository).readByID(headers, "S1");
        serviceReadByIdLiveData.setValue(mock(com.example.do_an_tot_nghiep.Container.ServiceReadByID.class));

        assertSame(serviceReadByIdLiveData, viewModelUnderTest.getServiceReadByIdResponse());
        assertNotNull(observedValue.get());
    }

    // Test Case ID: TC-VM-008
    // Real bug: BookingpageViewModel.serviceReadById() crashes when repository was not instantiated.
    @Test
    public void givenFreshBookingpageViewModelWhenServiceReadByIdCalledThenShouldNotCrash() {
        BookingpageViewModel viewModelUnderTest = new BookingpageViewModel();

        // This fails because serviceRepository is null unless instantiate() is called or the field is injected.
        viewModelUnderTest.serviceReadById(headers, "S1");
    }

    // Test Case ID: TC-VM-009
    // Valid repository interaction: AppointmentpageViewModel should expose repository LiveData values.
    @Test
    public void givenInjectedAppointmentRepositoriesWhenReadAllSuccessThenViewModelExposesRepositoryLiveData() throws Exception {
        AppointmentpageViewModel viewModelUnderTest = new AppointmentpageViewModel();
        SingleLiveEvent<AppointmentReadAll> repositoryLiveData = new SingleLiveEvent<>();
        MutableLiveData<Boolean> repositoryAnimation = new MutableLiveData<>();

        doReturn(repositoryAnimation).when(appointmentRepository).getAnimation();
        doReturn(repositoryLiveData).when(appointmentRepository).getReadAllResponse();
        setPrivateField(viewModelUnderTest, "repository", appointmentRepository);

        viewModelUnderTest.readAll(headers, parameters);
        AtomicReference<AppointmentReadAll> observedValue = observe(viewModelUnderTest.getReadAllResponse());
        verify(appointmentRepository).readAll(headers, parameters);
        repositoryLiveData.setValue(mock(AppointmentReadAll.class));

        assertSame(repositoryLiveData, viewModelUnderTest.getReadAllResponse());
        assertNotNull(observedValue.get());
    }

    // Test Case ID: TC-VM-010
    // Real bug: AppointmentpageViewModel.readAll() crashes when repository was not instantiated.
    @Test
    public void givenFreshAppointmentpageViewModelWhenReadAllCalledThenShouldNotCrash() {
        AppointmentpageViewModel viewModelUnderTest = new AppointmentpageViewModel();

        // This fails because repository is null unless instantiate() is called first.
        viewModelUnderTest.readAll(headers, parameters);
    }

    // Test Case ID: TC-VM-011
    // Valid repository interaction: HomepageViewModel should expose repository LiveData values.
    @Test
    public void givenInjectedHomepageRepositoriesWhenReadAllSuccessThenViewModelExposesRepositoryLiveData() throws Exception {
        HomepageViewModel viewModelUnderTest = new HomepageViewModel();
        SingleLiveEvent<DoctorReadAll> doctorLiveData = new SingleLiveEvent<>();
        SingleLiveEvent<com.example.do_an_tot_nghiep.Container.SpecialityReadAll> specialityLiveData = new SingleLiveEvent<>();
        MutableLiveData<Boolean> doctorAnimation = new MutableLiveData<>();
        MutableLiveData<Boolean> specialityAnimation = new MutableLiveData<>();

        doReturn(doctorAnimation).when(doctorRepository).getAnimation();
        doReturn(doctorLiveData).when(doctorRepository).getReadAllResponse();
        doReturn(specialityAnimation).when(specialityRepository).getAnimation();
        doReturn(specialityLiveData).when(specialityRepository).getReadAllResponse();
        setPrivateField(viewModelUnderTest, "doctorRepository", doctorRepository);
        setPrivateField(viewModelUnderTest, "specialityRepository", specialityRepository);

        viewModelUnderTest.doctorReadAll(headers, parameters);
        viewModelUnderTest.specialityReadAll(headers, parameters);
        AtomicReference<DoctorReadAll> observedDoctor = observe(viewModelUnderTest.getDoctorReadAllResponse());
        AtomicReference<com.example.do_an_tot_nghiep.Container.SpecialityReadAll> observedSpeciality = observe(viewModelUnderTest.getSpecialityReadAllResponse());
        verify(doctorRepository).readAll(headers, parameters);
        verify(specialityRepository).readAll(headers, parameters);
        doctorLiveData.setValue(mock(DoctorReadAll.class));
        specialityLiveData.setValue(mock(com.example.do_an_tot_nghiep.Container.SpecialityReadAll.class));

        assertSame(doctorLiveData, viewModelUnderTest.getDoctorReadAllResponse());
        assertSame(specialityLiveData, viewModelUnderTest.getSpecialityReadAllResponse());
        assertNotNull(observedDoctor.get());
        assertNotNull(observedSpeciality.get());
    }

    // Test Case ID: TC-VM-012
    // Real bug: HomepageViewModel.doctorReadAll() crashes when repositories were not instantiated.
    @Test
    public void givenFreshHomepageViewModelWhenDoctorReadAllCalledThenShouldNotCrash() {
        HomepageViewModel viewModelUnderTest = new HomepageViewModel();

        // This fails because doctorRepository/specialityRepository are null unless instantiate() is called first.
        viewModelUnderTest.doctorReadAll(headers, parameters);
    }

    private <T> AtomicReference<T> observe(MutableLiveData<T> liveData) {
        AtomicReference<T> observedValue = new AtomicReference<>();
        Observer<T> observer = observedValue::set;
        liveData.observeForever(observer);
        return observedValue;
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private <T> Call<T> mockCall() {
        return (Call<T>) mock(Call.class);
    }

    private <T> AtomicReference<Callback<T>> captureCallback(Call<T> call) {
        AtomicReference<Callback<T>> callbackRef = new AtomicReference<>();
        doAnswer(invocation -> {
            callbackRef.set(invocation.getArgument(0));
            return null;
        }).when(call).enqueue(any());
        return callbackRef;
    }
}


