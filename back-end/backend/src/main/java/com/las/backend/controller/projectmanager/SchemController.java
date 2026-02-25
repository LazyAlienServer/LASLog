package com.las.backend.controller.projectmanager;

import com.las.backend.model.projectmanager.MaterialReq;
import com.las.backend.service.projectmanager.SchemService;
import com.las.backend.utils.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@CrossOrigin("*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/project-manager")
public class SchemController {

    private final SchemService schemService;

    @GetMapping("/getSchemFiles")
    public Result getSchemFiles() throws ExecutionException, InterruptedException {return schemService.getSchemFiles();}

    @GetMapping("/getProgress")
    public Result getProgress(@RequestParam String filename) {
        return schemService.getProgress(filename);
    }

    @PostMapping("/getMissingMaterial")
    public Result getMissingMaterial(@RequestBody MaterialReq req) {return schemService.getMissingMaterial(req);}
}
