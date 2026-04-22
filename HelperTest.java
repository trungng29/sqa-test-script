package com.example.do_an_tot_nghiep;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import com.example.do_an_tot_nghiep.Helper.Tooltip;
import com.example.do_an_tot_nghiep.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class HelperTest {

    @Mock Context mockContext;
    @Mock SharedPreferences mockSharedPreferences;
    @Mock Resources mockResources;
    @Mock Configuration mockConfiguration;
    @Mock DisplayMetrics mockDisplayMetrics;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    // =========================================================
    // TC01–TC06 | getToday()
    // Không cần mock — static, pure Java
    // =========================================================

    /** TC01 — Trả về không null */
    @Test
    public void getToday_returnsNotNull() {
        String result = Tooltip.getToday();
        assertNotNull(result);
    }

    /** TC02 — Đúng format yyyy-MM-dd (regex) */
    @Test
    public void getToday_matchesFormat() {
        String result = Tooltip.getToday();
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    /** TC03 — Length = 10 */
    @Test
    public void getToday_hasLengthTen() {
        String result = Tooltip.getToday();
        assertEquals(10, result.length());
    }

    /** TC04 — Ngày < 10: phần ngày có padding "0" */
    @Test
    public void getToday_dayBelowTen_hasPaddedZero() {
        // Chạy nhiều ngày → khi ngày < 10, phần cuối phải là "0X"
        // Ở đây kiểm tra tổng quát: phần ngày (substring 8-10) là 2 chữ số
        String result = Tooltip.getToday();
        String dayPart = result.substring(8, 10);
        assertTrue(dayPart.matches("\\d{2}"));
    }

    /** TC05 — Tháng < 10: phần tháng có padding "0" */
    @Test
    public void getToday_monthBelowTen_hasPaddedZero() {
        String result = Tooltip.getToday();
        String monthPart = result.substring(5, 7);
        assertTrue(monthPart.matches("\\d{2}"));
    }

    /** TC06 — Ngày/tháng >= 10: không có prefix "0" không hợp lệ */
    @Test
    public void getToday_allPartsAreNumeric() {
        String result = Tooltip.getToday();
        // Xóa dấu "-" rồi kiểm tra còn lại là số
        String digits = result.replace("-", "");
        assertTrue(digits.matches("\\d{8}"));
    }


    // =========================================================
    // TC07–TC11 | getDateDifference()
    // Không cần mock — static, pure Java
    // =========================================================

    /** TC07 — date2 sau date1 1 ngày → trả về 1 (DAYS) */
    @Test
    public void getDateDifference_oneDayApart_inDays_returnsOne() {
        Date date1 = new Date(0L);
        Date date2 = new Date(86_400_000L); // +1 ngày
        long result = Tooltip.getDateDifference(date1, date2, TimeUnit.DAYS);
        assertEquals(1L, result);
    }

    /** TC08 — date1 = date2 → trả về 0 */
    @Test
    public void getDateDifference_sameDate_returnsZero() {
        Date date1 = new Date(0L);
        long result = Tooltip.getDateDifference(date1, date1, TimeUnit.DAYS);
        assertEquals(0L, result);
    }

    /** TC09 — date2 trước date1 → trả về số âm */
    @Test
    public void getDateDifference_date2BeforeDate1_returnsNegative() {
        Date date1 = new Date(86_400_000L);
        Date date2 = new Date(0L);
        long result = Tooltip.getDateDifference(date1, date2, TimeUnit.DAYS);
        assertEquals(-1L, result);
    }

    /** TC10 — Chênh lệch 1 ngày tính bằng HOURS → 24 */
    @Test
    public void getDateDifference_oneDayApart_inHours_returns24() {
        Date date1 = new Date(0L);
        Date date2 = new Date(86_400_000L);
        long result = Tooltip.getDateDifference(date1, date2, TimeUnit.HOURS);
        assertEquals(24L, result);
    }

    /** TC11 — Chênh lệch 90 phút tính bằng MINUTES → 90 */
    @Test
    public void getDateDifference_ninetyMinutes_inMinutes_returns90() {
        Date date1 = new Date(0L);
        Date date2 = new Date(90L * 60 * 1000); // 90 phút
        long result = Tooltip.getDateDifference(date1, date2, TimeUnit.MINUTES);
        assertEquals(90L, result);
    }


    // =========================================================
    // TC12–TC14 | getReadableToday(Context)
    // Cần mock Context (getString)
    // =========================================================

    /** TC12 — Trả về không null và không rỗng */
    @Test
    public void getReadableToday_returnsNotNullOrEmpty() {
        when(mockContext.getString(anyInt())).thenReturn("Thứ 2");

        String result = Tooltip.getReadableToday(mockContext);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    /** TC13 — Kết quả chứa dấu "/" phân cách ngày/tháng/năm */
    @Test
    public void getReadableToday_containsSlashSeparator() {
        when(mockContext.getString(anyInt())).thenReturn("Thứ 2");

        String result = Tooltip.getReadableToday(mockContext);

        assertTrue(result.contains("/"));
    }

    /** TC14 — Kết quả chứa tên thứ được trả về từ mock */
    @Test
    public void getReadableToday_containsDayName() {
        when(mockContext.getString(anyInt())).thenReturn("Thứ 2");

        String result = Tooltip.getReadableToday(mockContext);

        assertTrue(result.contains("Thứ 2"));
    }


    // =========================================================
    // TC15–TC19 | beautifierDatetime(Context, String)
    // Cần mock Context (getString) cho R.string.at
    // TC15 và TC19 không cần mock (kiểm tra trước khi dùng context)
    // =========================================================

    /** TC15 — Input length ≠ 19 (quá ngắn) → trả về chuỗi chứa "error" */
    @Test
    public void beautifierDatetime_inputTooShort_returnsError() {
        String result = Tooltip.beautifierDatetime(mockContext, "2022-11-24");
        assertTrue(result.contains("error"));
    }

    /** TC16 — Input hợp lệ → kết quả chứa phần giờ phút "09:57" */
    @Test
    public void beautifierDatetime_validInput_containsTimePart() {
        when(mockContext.getString(anyInt())).thenReturn("lúc");

        String result = Tooltip.beautifierDatetime(mockContext, "2022-11-24 09:57:53");

        assertTrue(result.contains("09:57"));
    }

    /** TC17 — Input hợp lệ → kết quả chứa từ nối từ mock context ("lúc") */
    @Test
    public void beautifierDatetime_validInput_containsAtWord() {
        when(mockContext.getString(anyInt())).thenReturn("lúc");

        String result = Tooltip.beautifierDatetime(mockContext, "2022-11-24 09:57:53");

        assertTrue(result.contains("lúc"));
    }

    /** TC18 — Input hợp lệ → kết quả không rỗng */
    @Test
    public void beautifierDatetime_validInput_notEmpty() {
        when(mockContext.getString(anyInt())).thenReturn("lúc");

        String result = Tooltip.beautifierDatetime(mockContext, "2022-11-24 09:57:53");

        assertFalse(result.isEmpty());
    }

    /** TC19 — Input length ≠ 19 (quá dài) → trả về chuỗi chứa "error" */
    @Test
    public void beautifierDatetime_inputTooLong_returnsError() {
        // 20 ký tự
        String result = Tooltip.beautifierDatetime(mockContext, "2022-11-24 09:57:531");
        assertTrue(result.contains("error"));
    }


    // =========================================================
    // TC20–TC22 | setLocale(Context, SharedPreferences)
    // Cần mock: Context, SharedPreferences, Resources,
    //           Configuration, DisplayMetrics
    // =========================================================

    private void setupSetLocaleMocks(String language) {
        when(mockContext.getString(R.string.vietnamese)).thenReturn("Tiếng Việt");
        when(mockContext.getString(R.string.deutsch)).thenReturn("Deutsch");
        when(mockSharedPreferences.getString(eq("language"), anyString()))
                .thenReturn(language);
        when(mockContext.getResources()).thenReturn(mockResources);
        when(mockResources.getDisplayMetrics()).thenReturn(mockDisplayMetrics);
        when(mockResources.getConfiguration()).thenReturn(mockConfiguration);
        // updateConfiguration là void → mặc định Mockito không làm gì (ok)
    }

    /** TC20 — Language = "Tiếng Việt" → không throw exception */
    @Test
    public void setLocale_vietnamese_doesNotThrow() {
        setupSetLocaleMocks("Tiếng Việt");

        // assertDoesNotThrow (JUnit 4 dùng cách này)
        try {
            Tooltip.setLocale(mockContext, mockSharedPreferences);
        } catch (Exception e) {
            fail("setLocale() threw an exception: " + e.getMessage());
        }
    }

    /** TC21 — Language = "Deutsch" → không throw exception */
    @Test
    public void setLocale_deutsch_doesNotThrow() {
        setupSetLocaleMocks("Deutsch");

        try {
            Tooltip.setLocale(mockContext, mockSharedPreferences);
        } catch (Exception e) {
            fail("setLocale() threw an exception: " + e.getMessage());
        }
    }

    /** TC22 — Language không khớp ("English") → fallback English, không throw */
    @Test
    public void setLocale_unknownLanguage_fallsBackToEnglish_doesNotThrow() {
        setupSetLocaleMocks("English");

        try {
            Tooltip.setLocale(mockContext, mockSharedPreferences);
        } catch (Exception e) {
            fail("setLocale() threw an exception: " + e.getMessage());
        }
    }
}