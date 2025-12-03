package tn.esprit.studentmanagement.services;

import org. junit.jupiter.api.Test;
import org.junit.jupiter. api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org. mockito.Mock;
import org.mockito.junit.jupiter. MockitoExtension;
import tn.esprit.studentmanagement.entities.Department;
import tn.esprit.studentmanagement.repositories.DepartmentRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit. jupiter.api.Assertions.*;
import static org.mockito. Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    public void testGetAllDepartments() {
        Department dept1 = new Department();
        dept1.setIdDepartment(1L);
        Department dept2 = new Department();
        dept2.setIdDepartment(2L);

        when(departmentRepository.findAll()). thenReturn(Arrays.asList(dept1, dept2));

        List<Department> result = departmentService.getAllDepartments();

        assertEquals(2, result. size());
        verify(departmentRepository, times(1)).findAll();
    }

    @Test
    public void testGetDepartmentById_Success() {
        Department dept = new Department();
        dept.setIdDepartment(1L);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));

        Department result = departmentService.getDepartmentById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getIdDepartment());
    }

    @Test
    public void testGetDepartmentById_NotFound() {
        when(departmentRepository.findById(999L)).thenReturn(Optional. empty());

        assertThrows(RuntimeException.class, () -> {
            departmentService.getDepartmentById(999L);
        });
    }

    @Test
    public void testSaveDepartment() {
        Department dept = new Department();
        when(departmentRepository.save(dept)).thenReturn(dept);

        Department result = departmentService. saveDepartment(dept);

        assertNotNull(result);
        verify(departmentRepository, times(1)).save(dept);
    }

    @Test
    public void testDeleteDepartment() {
        doNothing().when(departmentRepository).deleteById(1L);

        departmentService.deleteDepartment(1L);

        verify(departmentRepository, times(1)).deleteById(1L);
    }
}