package com.example.do_an_tot_nghiep.Container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.example.do_an_tot_nghiep.Model.Appointment;
import com.example.do_an_tot_nghiep.Model.Booking;
import com.example.do_an_tot_nghiep.Model.Doctor;
import com.example.do_an_tot_nghiep.Model.Main;
import com.example.do_an_tot_nghiep.Model.Notification;
import com.example.do_an_tot_nghiep.Model.Photo;
import com.example.do_an_tot_nghiep.Model.Queue;
import com.example.do_an_tot_nghiep.Model.Room;
import com.example.do_an_tot_nghiep.Model.Service;
import com.example.do_an_tot_nghiep.Model.Speciality;
import com.example.do_an_tot_nghiep.Model.Treatment;
import com.example.do_an_tot_nghiep.Model.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for Container/DTO classes.
 * These tests validate Gson mapping, boundary values, and nested object/list deserialization.
 */
public class ContainerModelsTest {

    private Gson gson;

    @Before
    public void setUp() {
        gson = new GsonBuilder().setLenient().create();
    }

    // Test Case ID: TC-CONTAINER-001
    // Valid weather payload maps scalar fields and nested Main object.
    @Test
    public void givenValidWeatherForecastJson_whenDeserialize_thenMapsNestedMainAndScalarFields() {
        String json = "{" +
                "\"timezone\":25200," +
                "\"id\":1581123," +
                "\"name\":\"Da Nang\"," +
                "\"cod\":\"200\"," +
                "\"main\":{\"temp\":27.5,\"feels_like\":29.0,\"temp_min\":26.0,\"pressure\":1008.0,\"humidity\":88.0}" +
                "}";

        WeatherForecast forecast = gson.fromJson(json, WeatherForecast.class);

        assertEquals(25200, forecast.getTimeZone());
        assertEquals(1581123, forecast.getId());
        assertEquals("Da Nang", forecast.getName());
        assertEquals("200", forecast.getCod());
        assertNotNull(forecast.getMain());
        assertEquals(27.5f, forecast.getMain().getTemp(), 0.0001f);
        assertEquals(29.0f, forecast.getMain().getFeelsLike(), 0.0001f);
    }

    // Test Case ID: TC-CONTAINER-002
    // Boundary value: empty queue data list should deserialize to an empty list, not crash.
    @Test
    public void givenEmptyAppointmentQueueJson_whenDeserialize_thenKeepsEmptyDataList() {
        String json = "{" +
                "\"result\":1," +
                "\"msg\":\"ok\"," +
                "\"quantity\":0," +
                "\"data\":[]" +
                "}";

        AppointmentQueue queue = gson.fromJson(json, AppointmentQueue.class);

        assertEquals(1, queue.getResult());
        assertEquals(0, queue.getQuantity());
        assertNotNull(queue.getData());
        assertEquals(0, queue.getData().size());
    }

    // Test Case ID: TC-CONTAINER-003
    // Valid list payload maps one appointment with nested doctor, speciality, and room.
    @Test
    public void givenValidAppointmentReadAllJson_whenDeserialize_thenMapsNestedObjectsAndList() {
        String json = "{" +
                "\"result\":1," +
                "\"msg\":\"ok\"," +
                "\"quantity\":1," +
                "\"data\":[{" +
                "\"id\":10," +
                "\"date\":\"2026-04-22\"," +
                "\"numerical_order\":2," +
                "\"position\":1," +
                "\"patient_id\":99," +
                "\"patient_name\":\"Nguyen Van A\"," +
                "\"patient_phone\":\"0123456789\"," +
                "\"patient_birthday\":\"2000-01-01\"," +
                "\"patient_reason\":\"Headache\"," +
                "\"appointment_time\":\"08:30\"," +
                "\"status\":\"pending\"," +
                "\"doctor\":{\"id\":7,\"email\":\"doctor@test.com\",\"phone\":\"0900000000\",\"name\":\"Dr One\",\"description\":\"General\",\"price\":\"500\",\"role\":\"doctor\",\"avatar\":\"avatar.png\",\"active\":\"1\",\"create_at\":\"2026-01-01\",\"update_at\":\"2026-01-02\",\"speciality\":{\"id\":2,\"name\":\"Cardio\",\"description\":\"Heart\",\"doctor_quantity\":4,\"image\":\"cardio.png\"},\"room\":{\"id\":9,\"name\":\"Room 1\",\"location\":\"Floor 1\"}}," +
                "\"speciality\":{\"id\":2,\"name\":\"Cardio\",\"description\":\"Heart\",\"doctor_quantity\":4,\"image\":\"cardio.png\"}," +
                "\"room\":{\"id\":9,\"name\":\"Room 1\",\"location\":\"Floor 1\"}" +
                "}]" +
                "}";

        AppointmentReadAll response = gson.fromJson(json, AppointmentReadAll.class);
        Appointment appointment = response.getData().get(0);

        assertEquals(1, response.getResult());
        assertEquals(1, response.getQuantity());
        assertEquals("ok", response.getMsg());
        assertEquals(1, response.getData().size());
        assertEquals("Nguyen Van A", appointment.getPatientName());
        assertNotNull(appointment.getDoctor());
        assertNotNull(appointment.getSpeciality());
        assertNotNull(appointment.getRoom());
        assertEquals("Dr One", appointment.getDoctor().getName());
        assertEquals("Room 1", appointment.getRoom().getName());
    }

    // Test Case ID: TC-CONTAINER-004
    // Valid nested appointment-by-id payload maps correctly.
    @Test
    public void givenValidAppointmentReadByIdJson_whenDeserialize_thenMapsNestedAppointment() {
        String json = "{" +
                "\"result\":1," +
                "\"msg\":\"ok\"," +
                "\"data\":{" +
                "\"id\":10,\"date\":\"2026-04-22\",\"numerical_order\":2,\"position\":1,\"patient_id\":99,\"patient_name\":\"Nguyen Van A\",\"patient_phone\":\"0123456789\",\"patient_birthday\":\"2000-01-01\",\"patient_reason\":\"Headache\",\"appointment_time\":\"08:30\",\"status\":\"pending\",\"doctor\":{\"id\":7,\"email\":\"doctor@test.com\",\"phone\":\"0900000000\",\"name\":\"Dr One\",\"description\":\"General\",\"price\":\"500\",\"role\":\"doctor\",\"avatar\":\"avatar.png\",\"active\":\"1\",\"create_at\":\"2026-01-01\",\"update_at\":\"2026-01-02\"},\"speciality\":{\"id\":2,\"name\":\"Cardio\",\"description\":\"Heart\",\"doctor_quantity\":4,\"image\":\"cardio.png\"},\"room\":{\"id\":9,\"name\":\"Room 1\",\"location\":\"Floor 1\"}" +
                "}" +
                "}";

        AppointmentReadByID response = gson.fromJson(json, AppointmentReadByID.class);

        assertEquals(1, response.getResult());
        assertEquals("ok", response.getMsg());
        assertNotNull(response.getData());
        assertEquals("Nguyen Van A", response.getData().getPatientName());
    }

    // Test Case ID: TC-CONTAINER-005
    // Valid booking read-all payload maps list entries and nested service object.
    @Test
    public void givenValidBookingReadAllJson_whenDeserialize_thenMapsListAndNestedService() {
        String json = "{" +
                "\"result\":1," +
                "\"msg\":\"ok\"," +
                "\"quantity\":1," +
                "\"data\":[{" +
                "\"id\":5,\"booking_name\":\"Nguyen Van A\",\"booking_phone\":\"0123456789\",\"name\":\"Patient A\",\"gender\":1,\"birthday\":\"2000-01-01\",\"address\":\"Ha Noi\",\"reason\":\"Checkup\",\"appointment_time\":\"08:30\",\"appointment_date\":\"2026-04-30\",\"status\":\"pending\",\"create_at\":\"2026-04-22\",\"update_at\":\"2026-04-22\",\"service\":{\"id\":3,\"name\":\"General Check\",\"image\":\"service.png\",\"description\":\"General exam\"}" +
                "}]" +
                "}";

        BookingReadAll response = gson.fromJson(json, BookingReadAll.class);
        Booking booking = response.getData().get(0);

        assertEquals(1, response.getQuantity());
        assertEquals("Nguyen Van A", booking.getBookingName());
        assertNotNull(booking.getService());
        assertEquals("General Check", booking.getService().getName());
    }

    // Test Case ID: TC-CONTAINER-006
    // Valid booking-by-id payload maps nested booking object.
    @Test
    public void givenValidBookingReadByIdJson_whenDeserialize_thenMapsNestedBooking() {
        String json = "{" +
                "\"result\":1," +
                "\"msg\":\"ok\"," +
                "\"data\":{\"id\":5,\"booking_name\":\"Nguyen Van A\",\"booking_phone\":\"0123456789\",\"name\":\"Patient A\",\"gender\":1,\"birthday\":\"2000-01-01\",\"address\":\"Ha Noi\",\"reason\":\"Checkup\",\"appointment_time\":\"08:30\",\"appointment_date\":\"2026-04-30\",\"status\":\"pending\",\"create_at\":\"2026-04-22\",\"update_at\":\"2026-04-22\",\"service\":{\"id\":3,\"name\":\"General Check\",\"image\":\"service.png\",\"description\":\"General exam\"}}" +
                "}";

        BookingReadByID response = gson.fromJson(json, BookingReadByID.class);

        assertNotNull(response.getData());
        assertEquals("Patient A", response.getData().getName());
        assertEquals("General Check", response.getData().getService().getName());
    }

    // Test Case ID: TC-CONTAINER-007
    // Valid booking photo payload maps booking details and photo list.
    @Test
    public void givenValidBookingPhotoReadAllJson_whenDeserialize_thenMapsBookingAndPhotos() {
        String json = "{" +
                "\"result\":1," +
                "\"quantity\":2," +
                "\"msg\":\"ok\"," +
                "\"booking\":{\"id\":5,\"booking_name\":\"Nguyen Van A\",\"booking_phone\":\"0123456789\",\"name\":\"Patient A\",\"gender\":1,\"birthday\":\"2000-01-01\",\"address\":\"Ha Noi\",\"reason\":\"Checkup\",\"appointment_time\":\"08:30\",\"appointment_date\":\"2026-04-30\",\"status\":\"pending\"}," +
                "\"data\":[{" +
                "\"id\":1,\"booking_id\":5,\"url\":\"https://example.com/1.jpg\"},{\"id\":2,\"booking_id\":5,\"url\":\"https://example.com/2.jpg\"}]" +
                "}";

        BookingPhotoReadAll response = gson.fromJson(json, BookingPhotoReadAll.class);

        assertEquals(2, response.getQuantity());
        assertNotNull(response.getBooking());
        assertEquals("Nguyen Van A", response.getBooking().getBookingName());
        assertEquals(2, response.getData().size());
        assertEquals("https://example.com/1.jpg", response.getData().get(0).getUrl());
    }

    // Test Case ID: TC-CONTAINER-008
    // Valid user payload maps login token and profile fields.
    @Test
    public void givenValidLoginJson_whenDeserialize_thenMapsAccessTokenAndUser() {
        String json = "{" +
                "\"result\":1," +
                "\"msg\":\"ok\"," +
                "\"accessToken\":\"token-123\"," +
                "\"data\":{\"id\":1,\"email\":\"patient@test.com\",\"phone\":\"0123456789\",\"name\":\"Patient A\",\"gender\":1,\"birthday\":\"2000-01-01\",\"address\":\"Ha Noi\",\"avatar\":\"avatar.png\",\"create_at\":\"2026-04-22\",\"update_at\":\"2026-04-22\"}" +
                "}";

        Login login = gson.fromJson(json, Login.class);

        assertEquals(Integer.valueOf(1), login.getResult());
        assertEquals("token-123", login.getAccessToken());
        assertNotNull(login.getData());
        assertEquals("Patient A", login.getData().getName());
    }

    // Test Case ID: TC-CONTAINER-009
    // Valid patient profile payload maps nested user object.
    @Test
    public void givenValidPatientProfileJson_whenDeserialize_thenMapsUser() {
        String json = "{" +
                "\"result\":1," +
                "\"msg\":\"ok\"," +
                "\"data\":{\"id\":1,\"email\":\"patient@test.com\",\"phone\":\"0123456789\",\"name\":\"Patient A\",\"gender\":1,\"birthday\":\"2000-01-01\",\"address\":\"Ha Noi\",\"avatar\":\"avatar.png\",\"create_at\":\"2026-04-22\",\"update_at\":\"2026-04-22\"}" +
                "}";

        PatientProfile profile = gson.fromJson(json, PatientProfile.class);

        assertEquals(Integer.valueOf(1), profile.getResult());
        assertNotNull(profile.getData());
        assertEquals("patient@test.com", profile.getData().getEmail());
    }

    // Test Case ID: TC-CONTAINER-010
    // Valid profile avatar change payload maps url and nested data.
    @Test
    public void givenValidPatientProfileChangeAvatarJson_whenDeserialize_thenMapsUrlAndUser() {
        String json = "{" +
                "\"result\":1," +
                "\"msg\":\"ok\"," +
                "\"url\":\"https://example.com/avatar.png\"," +
                "\"data\":{\"id\":1,\"email\":\"patient@test.com\",\"phone\":\"0123456789\",\"name\":\"Patient A\",\"gender\":1,\"birthday\":\"2000-01-01\",\"address\":\"Ha Noi\",\"avatar\":\"avatar.png\",\"create_at\":\"2026-04-22\",\"update_at\":\"2026-04-22\"}" +
                "}";

        PatientProfileChangeAvatar response = gson.fromJson(json, PatientProfileChangeAvatar.class);

        assertEquals("https://example.com/avatar.png", response.getUrl());
        assertNotNull(response.getData());
        assertEquals("Patient A", response.getData().getName());
    }

    // Test Case ID: TC-CONTAINER-011
    // Valid notification payload maps unread count and list data.
    @Test
    public void givenValidNotificationReadAllJson_whenDeserialize_thenMapsUnreadCountAndList() {
        String json = "{" +
                "\"result\":1," +
                "\"msg\":\"ok\"," +
                "\"quantity\":1," +
                "\"quantityUnread\":1," +
                "\"data\":[{" +
                "\"id\":3,\"message\":\"New record\",\"record_id\":7,\"record_type\":\"appointment\",\"is_read\":0,\"create_at\":\"2026-04-22\",\"update_at\":\"2026-04-22\"}]" +
                "}";

        NotificationReadAll response = gson.fromJson(json, NotificationReadAll.class);
        Notification notification = response.getData().get(0);

        assertEquals(1, response.getQuantityUnread());
        assertEquals("New record", notification.getMessage());
        assertEquals(0, notification.getIsRead());
    }

    // Test Case ID: TC-CONTAINER-012
    // Valid service list payload maps all service items.
    @Test
    public void givenValidServiceReadAllJson_whenDeserialize_thenMapsListEntries() {
        String json = "{" +
                "\"result\":1," +
                "\"msg\":\"ok\"," +
                "\"quantity\":2," +
                "\"data\":[{" +
                "\"id\":1,\"name\":\"X-Ray\",\"image\":\"xray.png\",\"description\":\"Scan\"},{\"id\":2,\"name\":\"Ultrasound\",\"image\":\"us.png\",\"description\":\"Ultrasound scan\"}]" +
                "}";

        ServiceReadAll response = gson.fromJson(json, ServiceReadAll.class);

        assertEquals(2, response.getQuantity());
        assertEquals("X-Ray", response.getData().get(0).getName());
        assertEquals("Ultrasound", response.getData().get(1).getName());
    }

    // Test Case ID: TC-CONTAINER-013
    // Valid speciality list payload maps entries correctly.
    @Test
    public void givenValidSpecialityReadAllJson_whenDeserialize_thenMapsListEntries() {
        String json = "{" +
                "\"result\":1," +
                "\"quantity\":1," +
                "\"data\":[{" +
                "\"id\":2,\"name\":\"Cardiology\",\"description\":\"Heart\",\"doctor_quantity\":4,\"image\":\"cardio.png\"}]" +
                "}";

        SpecialityReadAll response = gson.fromJson(json, SpecialityReadAll.class);

        assertEquals(1, response.getQuantity());
        assertEquals("Cardiology", response.getData().get(0).getName());
    }

    // Test Case ID: TC-CONTAINER-014
    // Valid treatment list payload maps entries correctly.
    @Test
    public void givenValidTreatmentReadAllJson_whenDeserialize_thenMapsListEntries() {
        String json = "{" +
                "\"result\":1," +
                "\"quantity\":1," +
                "\"data\":[{" +
                "\"id\":1,\"appointment_id\":9,\"name\":\"Aspirin\",\"type\":\"tablet\",\"times\":2,\"purpose\":\"Pain relief\",\"instruction\":\"After meal\",\"repeat_days\":\"5\",\"repeat_time\":\"08:00\"}]" +
                "}";

        TreatmentReadAll response = gson.fromJson(json, TreatmentReadAll.class);

        assertEquals(1, response.getQuantity());
        assertEquals("Aspirin", response.getData().get(0).getName());
        assertEquals(2, response.getData().get(0).getTimes());
    }

    // Test Case ID: TC-CONTAINER-015
    // Boundary value: simple scalar response containers should deserialize even with minimal payloads.
    @Test
    public void givenSimpleResponseContainers_whenDeserialize_thenMapScalarFields() {
        BookingCancel bookingCancel = gson.fromJson("{\"result\":1,\"msg\":\"cancelled\"}", BookingCancel.class);
        BookingPhotoDelete bookingPhotoDelete = gson.fromJson("{\"result\":1,\"msg\":\"deleted\"}", BookingPhotoDelete.class);
        BookingPhotoUpload bookingPhotoUpload = gson.fromJson("{\"result\":1,\"msg\":\"uploaded\",\"url\":\"https://example.com/p.png\"}", BookingPhotoUpload.class);
        NotificationCreate notificationCreate = gson.fromJson("{\"result\":1,\"msg\":\"created\"}", NotificationCreate.class);
        NotificationMarkAllAsRead markAllAsRead = gson.fromJson("{\"result\":1,\"msg\":\"ok\"}", NotificationMarkAllAsRead.class);
        NotificationMarkAsRead markAsRead = gson.fromJson("{\"result\":1,\"msg\":\"ok\"}", NotificationMarkAsRead.class);

        assertEquals(1, bookingCancel.getResult());
        assertEquals("cancelled", bookingCancel.getMsg());
        assertEquals("deleted", bookingPhotoDelete.getMsg());
        assertEquals("https://example.com/p.png", bookingPhotoUpload.getUrl());
        assertEquals("created", notificationCreate.getMsg());
        assertEquals("ok", markAllAsRead.getMsg());
        assertEquals("ok", markAsRead.getMsg());
    }

    // Test Case ID: TC-CONTAINER-016
    // Valid read-by-id containers should map nested payloads without crashing.
    @Test
    public void givenValidReadByIdContainers_whenDeserialize_thenMapNestedData() {
        DoctorReadByID doctorReadByID = gson.fromJson("{\"result\":1,\"msg\":\"ok\",\"data\":{\"id\":7,\"email\":\"doctor@test.com\",\"phone\":\"0900000000\",\"name\":\"Dr One\",\"description\":\"General\",\"price\":\"500\",\"role\":\"doctor\",\"avatar\":\"avatar.png\",\"active\":\"1\",\"create_at\":\"2026-04-22\",\"update_at\":\"2026-04-22\"}}", DoctorReadByID.class);
        ServiceReadByID serviceReadByID = gson.fromJson("{\"result\":1,\"msg\":\"ok\",\"data\":{\"id\":3,\"name\":\"X-Ray\",\"image\":\"xray.png\",\"description\":\"Scan\"}}", ServiceReadByID.class);
        SpecialityReadByID specialityReadByID = gson.fromJson("{\"result\":1,\"msg\":\"ok\",\"data\":{\"id\":2,\"name\":\"Cardiology\",\"description\":\"Heart\",\"doctor_quantity\":4,\"image\":\"cardio.png\"}}", SpecialityReadByID.class);
        TreatmentReadByID treatmentReadByID = gson.fromJson("{\"result\":1,\"msg\":\"ok\",\"data\":{\"id\":1,\"appointment_id\":9,\"name\":\"Aspirin\",\"type\":\"tablet\",\"times\":2,\"purpose\":\"Pain relief\",\"instruction\":\"After meal\",\"repeat_days\":\"5\",\"repeat_time\":\"08:00\"}}", TreatmentReadByID.class);
        RecordReadByID recordReadByID = gson.fromJson("{\"result\":1,\"msg\":\"ok\",\"data\":{\"id\":11,\"reason\":\"Checkup\",\"description\":\"Need follow-up\",\"status_before\":\"new\",\"status_after\":\"done\",\"create_at\":\"2026-04-22\",\"update_at\":\"2026-04-22\",\"appointment\":{\"id\":10,\"date\":\"2026-04-22\",\"numerical_order\":2,\"position\":1,\"patient_id\":99,\"patient_name\":\"Nguyen Van A\",\"patient_phone\":\"0123456789\",\"patient_birthday\":\"2000-01-01\",\"patient_reason\":\"Headache\",\"appointment_time\":\"08:30\",\"status\":\"pending\"},\"doctor\":{\"id\":7,\"email\":\"doctor@test.com\",\"phone\":\"0900000000\",\"name\":\"Dr One\",\"description\":\"General\",\"price\":\"500\",\"role\":\"doctor\",\"avatar\":\"avatar.png\",\"active\":\"1\",\"create_at\":\"2026-04-22\",\"update_at\":\"2026-04-22\"},\"speciality\":{\"id\":2,\"name\":\"Cardiology\",\"description\":\"Heart\",\"doctor_quantity\":4,\"image\":\"cardio.png\"}}}", RecordReadByID.class);

        assertNotNull(doctorReadByID.getData());
        assertNotNull(serviceReadByID.getData());
        assertNotNull(specialityReadByID.getData());
        assertNotNull(treatmentReadByID.getData());
        assertNotNull(recordReadByID.getData());
        assertEquals("Dr One", doctorReadByID.getData().getName());
        assertEquals("X-Ray", serviceReadByID.getData().getName());
        assertEquals("Cardiology", specialityReadByID.getData().getName());
        assertEquals("Aspirin", treatmentReadByID.getData().getName());
        assertEquals("Checkup", recordReadByID.getData().getReason());
    }
}

