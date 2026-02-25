package com.las.backend.controller.projectmanager;

import com.las.backend.model.projectmanager.MaterialReq;
import com.las.backend.service.projectmanager.SchemService;
import com.las.backend.utils.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@CrossOrigin("*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/project-manager")
@Tag(name = "项目管理", description = "项目管理接口")
public class SchemController {

    private final SchemService schemService;

    @GetMapping("/getSchemFiles")
    @Operation(summary = "获取原理图列表", description = "获取全部原理图列表")
    public Result getSchemFiles() throws ExecutionException, InterruptedException {
        return schemService.getSchemFiles();
    }

    @GetMapping("/getProgress")
    @Operation(summary = "获取进度", description = "获取全部方块数量与完成的方块数量以计算进度")
    @Parameter(name = "filename", description = "投影文件名", required = true)
    public Result getProgress(@RequestParam String filename) {
        return schemService.getProgress(filename);
    }

    @GetMapping("/getMissingMaterial")
    @Operation(summary = "获取缺失材料", description = "获取缺失材料,包含材料区域")
    public Result getMissingMaterial(@RequestBody MaterialReq req) {return schemService.getMissingMaterial(req);}
}
