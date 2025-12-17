package tn.esprit.studentmanagement.services;

import tn.esprit.studentmanagement.dto.SimpleStudentDTO;
import tn.esprit.studentmanagement.entities.Student;

import java.util.List;

public interface IStudentService {
    public List<Student> getAllStudents();
    public Student getStudentById(Long id);
    public Student saveStudent(Student student);
    public void deleteStudent(Long id);
    public List<SimpleStudentDTO> getAllSimpleStudentDTOs();
    public SimpleStudentDTO getSimpleStudentDTOById(Long id);
}
