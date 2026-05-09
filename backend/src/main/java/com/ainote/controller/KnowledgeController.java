package com.ainote.controller;

import com.ainote.common.Result;
import com.ainote.entity.Knowledge;
import com.ainote.service.KnowledgeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping
    public Result<List<Knowledge>> list() {
        return Result.success(knowledgeService.list());
    }

    @PostMapping
    public Result<Knowledge> create(@RequestBody Knowledge knowledge) {
        return Result.success(knowledgeService.create(knowledge));
    }

    @PutMapping("/{id}")
    public Result<Knowledge> update(@PathVariable Long id, @RequestBody Knowledge knowledge) {
        return Result.success(knowledgeService.update(id, knowledge));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.success();
    }
}
