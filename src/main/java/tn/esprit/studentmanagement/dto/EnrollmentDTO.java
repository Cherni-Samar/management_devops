package tn.esprit.studentmanagement.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EnrollmentDTO {
    private Long idEnrollment;
    private String enrollmentDate;
    private Double grade;
    private String status;
    private SimpleStudentDTO student; // seulement id + nom
    private SimpleCourseDTO course;   // seulement id + nom
}