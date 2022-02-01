package kup.get.controller;

import kup.get.config.ZipConfig;
import kup.get.entity.energy.Version;
import kup.get.service.VersionService;
import kup.get.service.ZipService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/superAdmin")
public class SuperAdminController {
    private final ZipService zipService;
    private final VersionService versionService;
    private final ZipConfig zipConfig;
    public SuperAdminController(ZipService zipService, VersionService versionService, ZipConfig zipConfig) {
        this.zipService = zipService;
        this.versionService = versionService;
        this.zipConfig = zipConfig;
    }

    @GetMapping(value = "/upload")
    public String provideUploadInfo() {
        return "superAdmin/updates";
    }

    @PostMapping(value = "/upload")
    public RedirectView handleFileUpload(@RequestParam("name") String name,
                                         @RequestParam("info") String info,
                                         @RequestParam("file") MultipartFile file,
                                         RedirectAttributes redirectAttributes) {
        System.out.println("HI");
        if (!file.isEmpty()) {

            zipService.update(versionService.save(name, info), file, zipConfig.getZipEntry());
        }
        return new RedirectView("/superAdmin/upload", true);
    }
}