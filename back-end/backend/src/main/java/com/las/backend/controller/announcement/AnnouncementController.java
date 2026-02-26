package com.las.backend.controller.announcement;

import com.las.backend.model.announcement.Article;
import com.las.backend.service.announcement.AnnouncementService;
import com.las.backend.utils.result.Result;
import com.las.backend.utils.result.ResultEnum;
import com.las.backend.utils.result.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Setter
@RestController
@RequestMapping("/announcement")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    public Result createArticle(@RequestBody Article article){
        Article savedArticle = announcementService.save(article);
        return ResultUtil.result(ResultEnum.SUCCESS.getCode(), savedArticle, "公告创建成功");
    }

    @GetMapping
    public Result getAllArticles() {
        List<Article> articles = announcementService.findAll();
        return ResultUtil.result(ResultEnum.SUCCESS.getCode(), articles, "获取文章列表成功"); //
    }

    @GetMapping("/{id}")
    public Result getArticleById(@PathVariable String id) {
        Optional<Article> articleOptional = announcementService.findById(id);
        return articleOptional.map(article -> ResultUtil.result(ResultEnum.SUCCESS.getCode(), article, "获取成功")).orElseGet(() -> ResultUtil.result(ResultEnum.NOT_FOUND.getCode(), "文章不存在"));
    }

    @PutMapping("/{id}")
    public Result updateArticle(@PathVariable String id, @RequestBody Article article) {
        if (!announcementService.existsById(id)) {
            return ResultUtil.result(ResultEnum.NOT_FOUND.getCode(), "更新失败：文章不存在"); //
        }
        article.setId(id);
        Article updatedArticle = announcementService.save(article);
        return ResultUtil.result(ResultEnum.SUCCESS.getCode(), updatedArticle, "文章更新成功"); //
    }

    @DeleteMapping("/{id}")
    public Result deleteArticle(@PathVariable String id) {
        if (!announcementService.existsById(id)) {
            return ResultUtil.result(ResultEnum.NOT_FOUND.getCode(), "删除失败：文章不存在"); //
        }
        announcementService.deleteById(id);
        return ResultUtil.result(ResultEnum.SUCCESS.getCode(), "文章删除成功"); //
    }


}
