package com.ainote.service;

import com.ainote.common.BusinessException;
import com.ainote.entity.Task;
import com.ainote.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private TaskService taskService;

    @Test
    void shouldTrimTitleAndApplyDefaultsWhenCreatingTask() {
        Task task = new Task();
        task.setTitle("  准备周报  ");
        task.setPriority(null);

        when(userContextService.getCurrentUserId()).thenReturn(5L);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task saved = taskService.create(task);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());

        assertEquals("准备周报", saved.getTitle());
        assertEquals("pending", saved.getStatus());
        assertEquals("medium", saved.getPriority());
        assertEquals(5L, captor.getValue().getUserId());
    }

    @Test
    void shouldRejectBlankTitle() {
        Task task = new Task();
        task.setTitle("   ");

        assertThrows(BusinessException.class, () -> taskService.create(task));
    }
}
