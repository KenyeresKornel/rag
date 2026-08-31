package com.example.arxivrag.paper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaperRepository extends JpaRepository<Paper, Long> {
    Optional<Paper> findByArxivId(String arxivId);
    List<Paper> findAllByArxivIdIn(Collection<String> arxivIds);
}
