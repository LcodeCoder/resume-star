package com.resume.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDate;
import java.sql.Date;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SmartResumeJobStoreTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final SmartResumeJobStore store = new SmartResumeJobStore(jdbc);
    private final LocalDate day = LocalDate.of(2026, 9, 24);

    @Test void reservesAfterLockingQuotaRowAndOnlyForNewJob() {
        when(jdbc.queryForList(startsWith("SELECT id FROM rl_smart_resume_job"), eq(String.class), eq(7L)))
                .thenReturn(List.of());
        when(jdbc.update(startsWith("UPDATE rl_smart_resume_usage SET used = used + 1"), eq(7L), eq(day), eq(5)))
                .thenReturn(1);
        SmartResumeJobStore.Reserved reserved = store.reserve(7L, day, 5);
        assertTrue(reserved.created());
        verify(jdbc).queryForObject(contains("FOR UPDATE"), eq(Integer.class), eq(7L), eq(day));
        verify(jdbc).update(startsWith("INSERT INTO rl_smart_resume_job"), eq(reserved.id()), eq(7L), eq(day));
    }

    @Test void activeTaskDoesNotReserveAnotherUse() {
        when(jdbc.queryForList(startsWith("SELECT id FROM rl_smart_resume_job"), eq(String.class), eq(7L)))
                .thenReturn(List.of("existing"));
        assertEquals(new SmartResumeJobStore.Reserved("existing", false), store.reserve(7L, day, 5));
        verify(jdbc, never()).update(startsWith("UPDATE rl_smart_resume_usage SET used = used + 1"), any(), any(), any());
    }

    @Test void failedJobRefundsOnceAndCannotRefundTerminalJob() {
        when(jdbc.update(startsWith("UPDATE rl_smart_resume_job SET status = 'FAILED'"),
                anyString(), eq("job"))).thenReturn(1, 0);
        store.fail("job", 7L, day, "超时，请重试");
        store.fail("job", 7L, day, "超时，请重试");
        verify(jdbc, times(1)).update(startsWith("UPDATE rl_smart_resume_usage SET used = GREATEST"), eq(7L), eq(day));
    }
    @Test void restartRefundsUnfinishedJobAndDoesNotRefundAgain() throws Exception {
        when(jdbc.query(startsWith("SELECT id, user_id, usage_day FROM rl_smart_resume_job"),
                any(RowMapper.class))).thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet row = mock(ResultSet.class);
                    when(row.getString("id")).thenReturn("interrupted");
                    when(row.getLong("user_id")).thenReturn(7L);
                    when(row.getDate("usage_day")).thenReturn(Date.valueOf(day));
                    return List.of(mapper.mapRow(row, 0));
                });
        when(jdbc.update(startsWith("UPDATE rl_smart_resume_job SET status = 'FAILED'"),
                contains("服务重启"), eq("interrupted"))).thenReturn(1, 0);
        store.refundInterruptedJobs();
        store.refundInterruptedJobs();
        verify(jdbc, times(1)).update(startsWith("UPDATE rl_smart_resume_usage SET used = GREATEST"), eq(7L), eq(day));
    }
}
