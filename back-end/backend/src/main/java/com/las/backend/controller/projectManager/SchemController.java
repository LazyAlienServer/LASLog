package com.las.backend.controller.projectManager;

import com.las.backend.model.projectmanager.MaterialReq;
import com.las.backend.service.projectmanager.SchemService;
import com.las.backend.utils.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/project-manager/")
public class SchemController {

    private final SchemService schemService;

    @GetMapping("getProgress")
    public Result getProgress(@RequestParam String filename) {
        return schemService.getProgress(filename);
    }

    @PostMapping("getMissingMaterial")
    public Result getMissingMaterial(@RequestBody MaterialReq req) {

        return schemService.getMissingMaterial(req);
    }
}
