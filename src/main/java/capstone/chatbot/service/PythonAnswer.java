package capstone.chatbot.service;

import java.util.Map;

record PythonAnswer(String answer, String usedUserId, Map<String, Object> meta) {
}
