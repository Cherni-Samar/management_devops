package tn.esprit.studentmanagement.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.studentmanagement.dto.EnrollmentDTO;
import tn.esprit.studentmanagement.dto.SimpleCourseDTO;
import tn.esprit.studentmanagement.dto.SimpleStudentDTO;
import tn.esprit.studentmanagement.repositories.EnrollmentRepository;
import tn.esprit.studentmanagement.entities.Enrollment;
import java.util.List;

@Service
public class EnrollmentService implements IEnrollment {
    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Override
    public List<Enrollment> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }

    @Override
    public Enrollment getEnrollmentById(Long idEnrollment) {
        return enrollmentRepository.findById(idEnrollment)
                .orElseThrow(() -> new RuntimeException("Enrollment not found with id: " + idEnrollment));
    }
    @Override
    public Enrollment saveEnrollment(Enrollment enrollment) {
        return enrollmentRepository.save(enrollment);
    }

    @Override
    public void deleteEnrollment(Long idEnrollment) {
enrollmentRepository.deleteById(idEnrollment);
    }

    @Override
    public EnrollmentDTO toDTO(Enrollment enrollment) {
        return new EnrollmentDTO(
                enrollment.getIdEnrollment(),
                enrollment.getEnrollmentDate() != null ? enrollment.getEnrollmentDate().toString() : null,
                enrollment.getGrade(),
                enrollment.getStatus().name(),
                new SimpleStudentDTO(
                        enrollment.getStudent().getIdStudent(),
                        enrollment.getStudent().getFirstName(),
                        enrollment.getStudent().getLastName(),
                        enrollment.getStudent().getEmail(),
                        enrollment.getStudent().getPhone()
                ),
                new SimpleCourseDTO(
                        enrollment.getCourse().getIdCourse(),
                        enrollment.getCourse().getName()
                )
        );
    }

}
