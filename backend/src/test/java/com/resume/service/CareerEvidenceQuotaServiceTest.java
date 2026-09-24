package com.resume.service;

import com.resume.entity.MemberPackageVO;
import com.resume.entity.UserProfileVO;
import com.resume.exception.BusinessException;
import com.resume.repository.InMemoryDataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CareerEvidenceQuotaServiceTest {
    private final InMemoryDataRepository repository = mock(InMemoryDataRepository.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final CareerEvidenceQuotaService quota = new CareerEvidenceQuotaService(repository, jdbc);

    @Test
    void freeAccountCannotReserveAndDoesNotTouchUsageTable() {
        when(repository.findUserById(42L)).thenReturn(UserProfileVO.builder().id(42L).build());
        assertThrows(BusinessException.class, () -> quota.reserve(42L));
        verifyNoInteractions(jdbc);
    }

    @Test
    void configuredPlanLimitIsEnforcedAndFailureCanBeRefunded() {
        when(repository.findUserById(42L)).thenReturn(UserProfileVO.builder().id(42L).vipLevel("专业会员").build());
        when(repository.listMemberPackages()).thenReturn(List.of(MemberPackageVO.builder()
                .name("专业会员").dailyEvidenceSearchQuota(2).build()));
        when(jdbc.update(contains("SET used = used + 1"), eq(42L), any(LocalDate.class), eq(2))).thenReturn(1);
        when(jdbc.queryForObject(contains("SELECT used"), eq(Integer.class), eq(42L), any(LocalDate.class))).thenReturn(1);
        assertEquals(1, quota.reserve(42L));
        quota.refund(42L);
        verify(jdbc).update(contains("GREATEST"), eq(42L), any(LocalDate.class));
        when(jdbc.update(contains("SET used = used + 1"), eq(42L), any(LocalDate.class), eq(2))).thenReturn(0);
        assertThrows(BusinessException.class, () -> quota.reserve(42L));
    }
}
