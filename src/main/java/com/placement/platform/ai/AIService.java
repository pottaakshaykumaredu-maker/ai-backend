package com.placement.platform.ai;

public interface AIService {
    String analyzeResume(String resumeText);
    String matchJob(String studentProfileJson, String jobRequirementsJson);
    String analyzeSkillGap(String currentSkills, String targetRole);
    String generateInterviewQuestions(String jobTitle, String companyName, String interviewType);
    String mockInterviewChat(String messageHistory, String targetRole);
}
