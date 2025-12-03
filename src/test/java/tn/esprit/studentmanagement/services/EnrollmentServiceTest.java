package tn.esprit.studentmanagement.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito. InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.studentmanagement.entities.Enrollment;
import tn.esprit. studentmanagement.repositories.EnrollmentRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org. junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Test
    public void testGetAllEnrollments() {
        Enrollment e1 = new Enrollment();
        Enrollment e2 = new Enrollment();
        when(enrollmentRepository.findAll()).thenReturn(Arrays.asList(e1, e2));

        List<Enrollment> result = enrollmentService. getAllEnrollments();

        assertEquals(2, result.size());
        verify(enrollmentRepository, times(1)).findAll();
    }

    @Test
    public void testGetEnrollmentById_Success() {
        Enrollment enrollment = new Enrollment();
        enrollment.setIdEnrollment(1L);
        when(enrollmentRepository.findById(1L)). thenReturn(Optional.of(enrollment));

        Enrollment result = enrollmentService.getEnrollmentById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getIdEnrollment());
    }

    @Test
    public void testGetEnrollmentById_NotFound() {
        when(enrollmentRepository.findById(999L)). thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            enrollmentService.getEnrollmentById(999L);
        });
    }

    @Test
    public void testSaveEnrollment() {
        Enrollment enrollment = new Enrollment();
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        Enrollment result = enrollmentService.saveEnrollment(enrollment);

        assertNotNull(result);
        verify(enrollmentRepository, times(1)).save(enrollment);
    }

    @Test
    public void testDeleteEnrollment() {
        doNothing(). when(enrollmentRepository).deleteById(1L);

        enrollmentService.deleteEnrollment(1L);

        verify(enrollmentRepository, times(1)).deleteById(1L);
    }
}