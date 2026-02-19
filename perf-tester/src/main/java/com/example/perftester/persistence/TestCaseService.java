package com.example.perftester.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;

    @Transactional(readOnly = true)
    public List<TestCase> listAll() {
        return testCaseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public TestCase getById(long id) {
        return testCaseRepository.findById(id)
                .orElseThrow(() -> new TestCaseNotFoundException(id));
    }

    @Transactional
    public TestCase create(String name, String message) {
        if (testCaseRepository.existsByName(name)) {
            throw new TestCaseNameConflictException(name);
        }
        var testCase = new TestCase();
        testCase.setName(name);
        testCase.setMessage(message);
        return testCaseRepository.save(testCase);
    }

    @Transactional
    public TestCase update(long id, String name, String message) {
        var testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new TestCaseNotFoundException(id));
        if (testCaseRepository.existsByNameAndIdNot(name, id)) {
            throw new TestCaseNameConflictException(name);
        }
        testCase.setName(name);
        testCase.setMessage(message);
        return testCaseRepository.save(testCase);
    }

    @Transactional
    public void delete(long id) {
        if (!testCaseRepository.existsById(id)) {
            throw new TestCaseNotFoundException(id);
        }
        testCaseRepository.deleteById(id);
    }
}
