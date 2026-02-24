package com.las.backend.controller.projectmanager;

import com.las.backend.model.projectmanager.MaterialReq;
import com.las.backend.service.projectmanager.SchemService;
import com.las.backend.utils.result.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchematicControllerTest {

    @Mock
    private SchemService schemService;

    @InjectMocks
    private SchemController schematicController;

    @Test
    void getProgress() {
        Result mockResult = new Result(200, "成功", null);
        when(schemService.getProgress(anyString())).thenReturn(mockResult);

        Result response = schematicController.getProgress("Test");

        assertEquals(200, response.getCode());
    }

    @Test
    void getMissingMaterial() {
        Result mockResult = new Result(200, "成功", null);
        when(schemService.getMissingMaterial(any(MaterialReq.class))).thenReturn(mockResult);

        Result response = schematicController.getMissingMaterial(new MaterialReq());

        assertEquals(200, response.getCode());
    }
}