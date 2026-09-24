package com.resume.service.impl;

import com.resume.entity.MemberPackageVO;
import com.resume.exception.BusinessException;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.InterviewService;
import com.resume.service.QuotaService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminMemberPackageTest {
    private final InMemoryDataRepository repository = mock(InMemoryDataRepository.class);
    private final AdminServiceImpl service = new AdminServiceImpl(repository,
            mock(InterviewService.class), mock(QuotaService.class));

    @Test void rejectOutOfRangeBeforeSaving() {
        for (int invalid : new int[]{-1, 1000}) {
            var pkg = MemberPackageVO.builder().dailySmartResumeQuota(invalid).build();
            assertEquals(400, assertThrows(BusinessException.class,
                    () -> service.saveMemberPackage(pkg)).getCode());
        }
        verify(repository, never()).saveMemberPackage(any());
    }

    @Test void oldClientPreservesExistingLimitOrDefaultsToFive() {
        var existing = MemberPackageVO.builder().id(9L).dailySmartResumeQuota(12).build();
        when(repository.getMemberPackage(9L)).thenReturn(existing);
        var update = MemberPackageVO.builder().id(9L).build();
        service.saveMemberPackage(update);
        assertEquals(12, update.getDailySmartResumeQuota());

        var created = new MemberPackageVO();
        service.saveMemberPackage(created);
        assertEquals(5, created.getDailySmartResumeQuota());
        verify(repository, times(2)).saveMemberPackage(any());
    }
}
