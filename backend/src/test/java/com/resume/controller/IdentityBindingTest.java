package com.resume.controller;

import com.resume.common.AiFeatureType;
import com.resume.entity.AiOptimizeRequest;
import com.resume.entity.ExportRecordRequest;
import com.resume.entity.SaveResumeRequest;
import com.resume.service.AiService;
import com.resume.service.ExportService;
import com.resume.service.ResumeService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IdentityBindingTest {
    @Test
    void resumeSaveAlwaysUsesSessionOwner() {
        ResumeService service = mock(ResumeService.class);
        SaveResumeRequest request = new SaveResumeRequest();
        request.setUserId(99L);
        request.setTitle("简历");
        new ResumeController(service).save(request, 42L);
        assertEquals(42L, request.getUserId());
        verify(service).saveResume(request);
    }

    @Test
    void aiOptimizationAlwaysUsesSessionQuotaOwner() {
        AiService service = mock(AiService.class);
        AiOptimizeRequest request = new AiOptimizeRequest();
        request.setUserId(99L);
        request.setFeatureType(AiFeatureType.POLISH);
        request.setContent("简历");
        new AiController(service).optimize(request, 42L);
        assertEquals(42L, request.getUserId());
        verify(service).optimize(request);
    }

    @Test
    void exportRecordAlwaysUsesSessionQuotaOwner() {
        ExportService service = mock(ExportService.class);
        ExportRecordRequest request = new ExportRecordRequest();
        request.setUserId(99L);
        request.setResumeId(3L);
        request.setExportType("PDF");
        new ExportController(service).record(request, 42L);
        assertEquals(42L, request.getUserId());
        verify(service).recordExport(request);
    }
}
