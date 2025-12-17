package tn.esprit.studentmanagement.services;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.studentmanagement.dto.SimpleStudentDTO;
import tn.esprit.studentmanagement.entities.Student;
import tn.esprit.studentmanagement.repositories.StudentRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentService implements IStudentService {
    @Autowired
    private StudentRepository studentRepository;

    public List<Student> getAllStudents() { return studentRepository.findAll(); }
    public Student getStudentById(Long id) { return studentRepository.findById(id).orElse(null); }
    public Student saveStudent(Student student) { return studentRepository.save(student); }
    public void deleteStudent(Long id) { studentRepository.deleteById(id); }
    public List<SimpleStudentDTO> getAllSimpleStudentDTOs() {
        return studentRepository.findAll().stream()
                .map(student -> new SimpleStudentDTO(
                        student.getIdStudent(),
                        student.getFirstName(),
                        student.getLastName(),
                        student.getEmail(),
                        student.getPhone()
                ))
                .collect(Collectors.toList());
    }
    @Override
    public SimpleStudentDTO getSimpleStudentDTOById(Long id) {
        Student student = studentRepository.findById(id).orElse(null);
        if (student == null) return null; // Ou lancer une exception pour gérer le not found !
        return new SimpleStudentDTO(
                student.getIdStudent(),
                student.getFirstName(),
                student.getLastName(),
                student.getEmail(),
                student.getPhone()
        );
    }
}

