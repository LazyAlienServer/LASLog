package com.las.backend.controller.announcement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.las.backend.model.announcement.Article;
import com.las.backend.service.announcement.AnnouncementService;
import com.las.backend.utils.result.ResultEnum;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnnouncementController.class)
class AnnouncementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnnouncementService announcementService;

    @Autowired
    private ObjectMapper objectMapper;

    // ================= 1. 测试: 创建文章 (POST) =================
    @Test
    void testCreateArticle() throws Exception {
        Article requestArticle = new Article(null, "新公告", "公告内容");
        Article savedArticle = new Article("65dd1a2b3c", "新公告", "公告内容");

        Mockito.when(announcementService.save(Mockito.any(Article.class))).thenReturn(savedArticle);

        mockMvc.perform(post("/announcement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestArticle)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.msg").value("公告创建成功"))
                .andExpect(jsonPath("$.data.id").value("65dd1a2b3c"))
                .andExpect(jsonPath("$.data.title").value("新公告"));
    }

    // ================= 2. 测试: 获取全部文章 (GET) =================
    @Test
    void testGetAllArticles() throws Exception {
        List<Article> articles = Arrays.asList(
                new Article("id-1", "标题1", "内容1"),
                new Article("id-2", "标题2", "内容2")
        );

        Mockito.when(announcementService.findAll()).thenReturn(articles);

        mockMvc.perform(get("/announcement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.msg").value("获取文章列表成功"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value("id-1"))
                .andExpect(jsonPath("$.data[1].title").value("标题2"));
    }

    // ================= 3. 测试: 根据ID获取单篇文章 (GET) =================
    @Test
    void testGetArticleById_Success() throws Exception {
        String articleId = "id-1";
        Article mockArticle = new Article(articleId, "现有标题", "现有内容");

        Mockito.when(announcementService.findById(articleId)).thenReturn(Optional.of(mockArticle));

        mockMvc.perform(get("/announcement/" + articleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.msg").value("获取成功"))
                .andExpect(jsonPath("$.data.id").value(articleId));
    }

    @Test
    void testGetArticleById_NotFound() throws Exception {
        String fakeId = "不存在的ID";

        Mockito.when(announcementService.findById(fakeId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/announcement/" + fakeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultEnum.NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.msg").value("文章不存在"));
    }

    // ================= 4. 测试: 更新文章 (PUT) =================
    @Test
    void testUpdateArticle_Success() throws Exception {
        String articleId = "id-1";
        Article updateData = new Article(null, "修改后的标题", "修改后的内容");
        Article savedData = new Article(articleId, "修改后的标题", "修改后的内容");

        // 模拟：文章存在
        Mockito.when(announcementService.existsById(articleId)).thenReturn(true);
        // 模拟：保存成功后返回
        Mockito.when(announcementService.save(Mockito.any(Article.class))).thenReturn(savedData);

        mockMvc.perform(put("/announcement/" + articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.msg").value("文章更新成功"))
                .andExpect(jsonPath("$.data.title").value("修改后的标题"));
    }

    @Test
    void testUpdateArticle_NotFound() throws Exception {
        String fakeId = "不存在的ID";
        Article updateData = new Article(null, "标题", "内容");

        // 模拟：文章不存在
        Mockito.when(announcementService.existsById(fakeId)).thenReturn(false);

        mockMvc.perform(put("/announcement/" + fakeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultEnum.NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.msg").value("更新失败：文章不存在"));
    }

    // ================= 5. 测试: 删除文章 (DELETE) =================
    @Test
    void testDeleteArticle_Success() throws Exception {
        String articleId = "id-1";

        // 模拟：文章存在
        Mockito.when(announcementService.existsById(articleId)).thenReturn(true);
        // deleteById 没有返回值，默认执行即可

        mockMvc.perform(delete("/announcement/" + articleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.msg").value("文章删除成功"));

        // 可选：验证 deleteById 方法确实被调用了 1 次
        Mockito.verify(announcementService, Mockito.times(1)).deleteById(articleId);
    }

    @Test
    void testDeleteArticle_NotFound() throws Exception {
        String fakeId = "不存在的ID";

        // 模拟：文章不存在
        Mockito.when(announcementService.existsById(fakeId)).thenReturn(false);

        mockMvc.perform(delete("/announcement/" + fakeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultEnum.NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.msg").value("删除失败：文章不存在"));

        // 可选：验证 deleteById 绝对没有被调用
        Mockito.verify(announcementService, Mockito.never()).deleteById(fakeId);
    }
}