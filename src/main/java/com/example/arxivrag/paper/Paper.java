package com.example.arxivrag.paper;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "papers")
public class Paper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "arxiv_id", unique = true, nullable = false, length = 50)
    private String arxivId;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "abstract_text", nullable = false, columnDefinition = "TEXT")
    private String abstractText;

    @Column(name = "authors", nullable = false)
    private List<String> authors;

    @Column(name = "categories", nullable = false)
    private List<String> categories;

    @Column(name = "submitted_date", nullable = false)
    private LocalDate submittedDate;

    @Column(name = "doi", length = 100)
    private String doi;

    @Column(name = "journal_ref", columnDefinition = "TEXT")
    private String journalRef;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // Default constructor for JPA
    public Paper() {
    }

    // Full constructor
    public Paper(String arxivId, String title, String abstractText, List<String> authors, List<String> categories, LocalDate submittedDate, String doi, String journalRef) {
        this.arxivId = arxivId;
        this.title = title;
        this.abstractText = abstractText;
        this.authors = authors;
        this.categories = categories;
        this.submittedDate = submittedDate;
        this.doi = doi;
        this.journalRef = journalRef;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getArxivId() {
        return arxivId;
    }

    public void setArxivId(String arxivId) {
        this.arxivId = arxivId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public LocalDate getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(LocalDate submittedDate) {
        this.submittedDate = submittedDate;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getJournalRef() {
        return journalRef;
    }

    public void setJournalRef(String journalRef) {
        this.journalRef = journalRef;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
