package dev.rota.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EmployeeTest {

    @Test
    void sameIdAndNameAreEqual() {
        assertEquals(new Employee("E1", "Riley Quinn"), new Employee("E1", "Riley Quinn"));
    }

    @Test
    void differentNameIsNotEqual() {
        assertNotEquals(new Employee("E1", "Riley Quinn"), new Employee("E1", "Morgan Blake"));
    }
}
