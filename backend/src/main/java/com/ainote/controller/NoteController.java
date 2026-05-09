package com.ainote.controller;

import com.ainote.common.Result;
import com.ainote.entity.QuickNote;
import com.ainote.service.NoteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public Result<List<QuickNote>> list() {
        return Result.success(noteService.list());
    }

    @PostMapping
    public Result<QuickNote> create(@RequestBody Map<String, String> request) {
        String content = request.get("content");
        return Result.success(noteService.create(content));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noteService.delete(id);
        return Result.success();
    }
}