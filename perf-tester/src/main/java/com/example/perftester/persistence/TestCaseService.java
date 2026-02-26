package com.example.perftester.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final HeaderTemplateRepository headerTemplateRepository;
    private final ResponseTemplateRepository responseTemplateRepository;

    @Transactional(readOnly = true)
    public List<TestCaseDetail> listAll() {
        return testCaseRepository.findAll().stream()
                .map(this::toDetail)
                .toList();
    }

    @Transactional(readOnly = true)
    public TestCaseDetail getById(long id) {
        return toDetail(testCaseRepository.findById(id)
                .orElseThrow(() -> new TestCaseNotFoundException(id)));
    }

    @Transactional
    public TestCaseDetail create(String name, String message,
                                 Long headerTemplateId, Long responseTemplateId) {
        if (testCaseRepository.existsByName(name)) {
            throw new TestCaseNameConflictException(name);
        }
        var testCase = new TestCase();
        testCase.setName(name);
        testCase.setMessage(message);
        applyTemplates(testCase, headerTemplateId, responseTemplateId);
        return toDetail(testCaseRepository.save(testCase));
    }

    @Transactional
    public TestCaseDetail update(long id, String name, String message,
                                 Long headerTemplateId, Long responseTemplateId) {
        var testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new TestCaseNotFoundException(id));
        if (testCaseRepository.existsByNameAndIdNot(name, id)) {
            throw new TestCaseNameConflictException(name);
        }
        testCase.setName(name);
        testCase.setMessage(message);
        applyTemplates(testCase, headerTemplateId, responseTemplateId);
        return toDetail(testCaseRepository.save(testCase));
    }

    @Transactional
    public void delete(long id) {
        if (!testCaseRepository.existsById(id)) {
            throw new TestCaseNotFoundException(id);
        }
        testCaseRepository.deleteById(id);
    }

    private void applyTemplates(TestCase testCase, Long headerTemplateId, Long responseTemplateId) {
        testCase.setHeaderTemplate(headerTemplateId != null
                ? headerTemplateRepository.findById(headerTemplateId).orElse(null)
                : null);
        testCase.setResponseTemplate(responseTemplateId != null
                ? responseTemplateRepository.findById(responseTemplateId).orElse(null)
                : null);
    }

    private TestCaseDetail toDetail(TestCase tc) {
        var ht = tc.getHeaderTemplate();
        var rt = tc.getResponseTemplate();
        return new TestCaseDetail(
                tc.getId(), tc.getName(), tc.getMessage(),
                ht != null ? ht.getId() : null,
                ht != null ? ht.getName() : null,
                rt != null ? rt.getId() : null,
                rt != null ? rt.getName() : null,
                tc.getCreatedAt(), tc.getUpdatedAt());
    }

    public record TestCaseDetail(long id, String name, String message,
                                 Long headerTemplateId, String headerTemplateName,
                                 Long responseTemplateId, String responseTemplateName,
                                 Instant createdAt, Instant updatedAt) {
    }
}
