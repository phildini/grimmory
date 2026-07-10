package org.booklore.service.book;

import org.booklore.config.security.service.AuthenticationService;
import org.booklore.model.dto.BookLoreUser;
import org.booklore.model.dto.NotebookEntry;
import org.booklore.repository.NotebookEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotebookServiceTest {

    @Mock
    private NotebookEntryRepository repository;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private NotebookService notebookService;

    private final Long userId = 1L;
    private BookLoreUser user;

    @BeforeEach
    void setUp() {
        user = BookLoreUser.builder().id(userId).isDefaultPassword(false).build();
    }

    private NotebookEntryRepository.EntryProjection projection(Long id, String type, String cfi, String text, String note,
                                                                String chapterTitle, Long bookId, String bookTitle,
                                                                String primaryBookType) {
        return new NotebookEntryRepository.EntryProjection() {
            @Override public Long getId() { return id; }
            @Override public String getType() { return type; }
            @Override public Long getBookId() { return bookId; }
            @Override public String getBookTitle() { return bookTitle; }
            @Override public String getText() { return text; }
            @Override public String getNote() { return note; }
            @Override public String getColor() { return null; }
            @Override public String getStyle() { return null; }
            @Override public String getChapterTitle() { return chapterTitle; }
            @Override public String getCfi() { return cfi; }
            @Override public String getPrimaryBookType() { return primaryBookType; }
            @Override public LocalDateTime getCreatedAt() { return LocalDateTime.now(); }
            @Override public LocalDateTime getUpdatedAt() { return LocalDateTime.now(); }
        };
    }

    @Test
    void getNotebookEntries_mapsCfiFromProjection() {
        NotebookEntryRepository.EntryProjection proj = projection(
                1L, "HIGHLIGHT", "epubcfi(/6/4!/4/14)",
                "highlighted text", null, "Chapter 1",
                10L, "Test Book", "EPUB"
        );
        Page<NotebookEntryRepository.EntryProjection> page = new PageImpl<>(List.of(proj));

        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(repository.findEntries(eq(userId), any(), eq(null), any(), any(Pageable.class))).thenReturn(page);

        Page<NotebookEntry> result = notebookService.getNotebookEntries(0, 50, Set.of("HIGHLIGHT"), null, null, "desc");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        NotebookEntry entry = result.getContent().getFirst();
        assertEquals("epubcfi(/6/4!/4/14)", entry.getCfi());
        assertEquals("HIGHLIGHT", entry.getType());
        assertEquals("highlighted text", entry.getText());
    }

    @Test
    void getNotebookEntries_withNullCfi_returnsNullCfi() {
        NotebookEntryRepository.EntryProjection proj = projection(
                2L, "BOOKMARK", null,
                "My Bookmark", null, null,
                10L, "Test Book", "EPUB"
        );
        Page<NotebookEntryRepository.EntryProjection> page = new PageImpl<>(List.of(proj));

        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(repository.findEntries(eq(userId), any(), eq(null), any(), any(Pageable.class))).thenReturn(page);

        Page<NotebookEntry> result = notebookService.getNotebookEntries(0, 50, Set.of("BOOKMARK"), null, null, "desc");

        assertNull(result.getContent().getFirst().getCfi());
    }

    @Test
    void getNotebookEntries_withMultipleTypes_returnsAllMapped() {
        NotebookEntryRepository.EntryProjection highlight = projection(
                1L, "HIGHLIGHT", "cfi:highlight",
                "highlight", null, "Ch1",
                10L, "Test Book", "EPUB"
        );
        NotebookEntryRepository.EntryProjection note = projection(
                2L, "NOTE", "cfi:note",
                "selected text", "my note", "Ch2",
                10L, "Test Book", "EPUB"
        );
        Page<NotebookEntryRepository.EntryProjection> page = new PageImpl<>(List.of(highlight, note));

        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(repository.findEntries(eq(userId), eq(Set.of("HIGHLIGHT", "NOTE")), eq(null), any(), any(Pageable.class)))
                .thenReturn(page);

        Page<NotebookEntry> result = notebookService.getNotebookEntries(
                0, 50, Set.of("HIGHLIGHT", "NOTE"), null, null, "desc"
        );

        assertEquals(2, result.getContent().size());
        assertEquals("cfi:highlight", result.getContent().get(0).getCfi());
        assertEquals("cfi:note", result.getContent().get(1).getCfi());
    }

    @Test
    void getAllNotebookEntries_mapsCfiForExport() {
        NotebookEntryRepository.EntryProjection proj = projection(
                3L, "NOTE", "epubcfi(/6/8!/4/20)",
                "selected", "note body", "Chapter 3",
                10L, "Test Book", "EPUB"
        );
        Page<NotebookEntryRepository.EntryProjection> page = new PageImpl<>(List.of(proj));

        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(repository.findEntries(eq(userId), eq(Set.of("NOTE")), eq(null), any(), any(Pageable.class)))
                .thenReturn(page);

        List<NotebookEntry> result = notebookService.getAllNotebookEntries(Set.of("NOTE"), null, null, "desc");

        assertEquals(1, result.size());
        assertEquals("epubcfi(/6/8!/4/20)", result.getFirst().getCfi());
    }

    @Test
    void getBooksWithAnnotations_returnsBookOptions() {
        NotebookEntryRepository.BookProjection bookProj = new NotebookEntryRepository.BookProjection() {
            @Override public Long getBookId() { return 10L; }
            @Override public String getBookTitle() { return "Test Book"; }
        };

        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(repository.findBooksWithAnnotations(eq(userId), any(), any(Pageable.class)))
                .thenReturn(List.of(bookProj));

        var result = notebookService.getBooksWithAnnotations(null);

        assertEquals(1, result.size());
        assertEquals(10L, result.getFirst().getBookId());
        assertEquals("Test Book", result.getFirst().getBookTitle());
    }

    @Test
    void getNotebookEntries_withBookFilter_passesBookIdToRepository() {
        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(repository.findEntries(eq(userId), any(), eq(10L), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        notebookService.getNotebookEntries(0, 50, Set.of("HIGHLIGHT"), 10L, null, "desc");

        verify(repository).findEntries(eq(userId), any(), eq(10L), any(), any(Pageable.class));
    }

    @Test
    void getNotebookEntries_withSearchQuery_trimsAndWrapsSearch() {
        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(repository.findEntries(eq(userId), any(), eq(null), eq("%find me%"), any(Pageable.class)))
                .thenReturn(Page.empty());

        notebookService.getNotebookEntries(0, 50, Set.of("HIGHLIGHT"), null, "  find me  ", "desc");

        verify(repository).findEntries(eq(userId), any(), eq(null), eq("%find me%"), any(Pageable.class));
    }

    @Test
    void getNotebookEntries_ascSort_createsAscendingSort() {
        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(repository.findEntries(eq(userId), any(), eq(null), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        notebookService.getNotebookEntries(0, 25, Set.of("HIGHLIGHT"), null, null, "asc");

        verify(repository).findEntries(eq(userId), any(), eq(null), any(),
                argThat((Pageable p) -> p.getSort().isAscending()));
    }

    @Test
    void getNotebookEntries_descSort_createsDescendingSort() {
        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(repository.findEntries(eq(userId), any(), eq(null), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        notebookService.getNotebookEntries(0, 25, Set.of("HIGHLIGHT"), null, null, "desc");

        verify(repository).findEntries(eq(userId), any(), eq(null), any(),
                argThat((Pageable p) -> p.getSort().isDescending()));
    }
}
