package com.ainote.controller;

import com.ainote.common.Result;
import com.ainote.dto.NoteCreateRequest;
import com.ainote.entity.QuickNote;
import com.ainote.service.NoteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public Result<QuickNote> create(@RequestBody NoteCreateRequest request) {
        return Result.success(noteService.create(request));
    }

    @PutMapping("/{id}")
    public Result<QuickNote> update(@PathVariable Long id, @RequestBody NoteCreateRequest request) {
        return Result.success(noteService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noteService.delete(id);
        return Result.success();
    }
}
