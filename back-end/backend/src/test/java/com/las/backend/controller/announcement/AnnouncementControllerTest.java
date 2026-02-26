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

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(AnnouncementController.class) // 只加载 Controller 层的上下文，速度极快
class AnnouncementControllerTest {

    @Autowired
    private MockMvc mockMvc; // 用于模拟发起 HTTP 请求

    @MockitoBean
    private AnnouncementService announcementService; // 模拟 Service，不连真实数据库

    @Autowired
    private ObjectMapper objectMapper; // 用于将 Java 对象转换为 JSON 字符串


    @Test
    void testCreateArticle() throws Exception {
        // 1. 准备测试数据 (前端传来的不带 ID 的文章)
        Article requestArticle = new Article(null, "测试标题", "测试内容");

        // 模拟数据库保存后返回带 ID 的文章
        Article savedArticle = new Article("65dd1a2b3c", "测试标题", "测试内容");

        // 定义 Mock 行为：当调用 save 时，返回 savedArticle
        Mockito.when(announcementService.save(Mockito.any(Article.class))).thenReturn(savedArticle);

        // 2. 发起 POST 请求并验证结果
        mockMvc.perform(post("/announcement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestArticle)))
                .andExpect(status().isOk()) // 期望 HTTP 状态码为 200
                .andExpect(jsonPath("$.code").value(ResultEnum.SUCCESS.getCode())) // 期望返回的 Result code 为 200
                .andExpect(jsonPath("$.msg").value("公告创建成功"))
                .andExpect(jsonPath("$.data.id").value("65dd1a2b3c")) // 期望数据中包含生成的 ID
                .andExpect(jsonPath("$.data.title").value("测试标题"));
    }

    @Test
    void testGetArticleById_Success() throws Exception {
        // 1. 准备数据
        String articleId = "65dd1a2b3c";
        Article mockArticle = new Article(articleId, "现有标题", "现有内容");

        // 定义 Mock 行为：当按 ID 查找时，返回这篇存在的文章
        Mockito.when(announcementService.findById(articleId)).thenReturn(Optional.of(mockArticle));

        // 2. 发起 GET 请求并验证
        mockMvc.perform(get("/announcement/" + articleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.title").value("现有标题"));
    }

    @Test
    void testGetArticleById_NotFound() throws Exception {
        // 1. 准备数据
        String fakeId = "999999";

        // 定义 Mock 行为：当查找不存在的 ID 时，返回 Empty
        Mockito.when(announcementService.findById(fakeId)).thenReturn(Optional.empty());

        // 2. 发起 GET 请求并验证你的 404 逻辑
        mockMvc.perform(get("/announcement/" + fakeId))
                .andExpect(status().isOk()) // 接口本身是通的，所以 HTTP 还是 200
                .andExpect(jsonPath("$.code").value(ResultEnum.NOT_FOUND.getCode())) // 但你自定义的 Result.code 应该是 404
                .andExpect(jsonPath("$.msg").value("文章不存在"));
    }

    @Test
    void testDeleteArticle() throws Exception {
        String articleId = "12345";

        // 定义 Mock 行为：假设文章存在
        Mockito.when(announcementService.existsById(articleId)).thenReturn(true);
        // deleteById 没有返回值，默认执行即可

        // 发起 DELETE 请求并验证
        mockMvc.perform(delete("/announcement/" + articleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.msg").value("文章删除成功"));
    }
}