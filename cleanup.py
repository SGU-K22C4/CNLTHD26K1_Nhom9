file_path = r'd:\hoctap\Nam_4\HK2\CNLTHienDai_ThayTai\Project\CNLTHD26K1_Nhom9\backend\services\chatbot-service\src\main\java\com\fashion\chatbotservice\service\impl\ChatbotServiceImpl.java'

with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Remove lines 231-494 (1-indexed), which is 230-493 in 0-indexed
# These are the executeHeuristicFallback method block
lines_to_remove = set(range(230, 494))  # 0-indexed: lines 231-494

kept_lines = [line for i, line in enumerate(lines) if i not in lines_to_remove]

# Also remove the imports for IntentClassifierService since it's no longer used
kept_lines = [line for line in kept_lines 
              if 'import com.fashion.chatbotservice.service.IntentClassifierService;' not in line]

# Remove unused @PostConstruct import if not needed elsewhere
import_post_construct_used = any('@PostConstruct' in line for line in kept_lines)
if not import_post_construct_used:
    kept_lines = [line for line in kept_lines 
                  if 'import jakarta.annotation.PostConstruct;' not in line]

# Remove private final IntentClassifierService field
kept_lines = [line for line in kept_lines 
              if 'private final IntentClassifierService intentClassifierService;' not in line]

# Remove bootstrapTrainingData method (if any remaining reference to intentClassifierService)
# Find and remove the @PostConstruct bootstrapTrainingData method
result = []
skip = False
for line in kept_lines:
    if '    @PostConstruct' in line:
        skip = True
    if skip and '    }' in line:
        skip = False
        continue  # skip the closing brace too
    if not skip:
        result.append(line)

with open(file_path, 'w', encoding='utf-8') as f:
    f.writelines(result)

print(f"Done. Removed executeHeuristicFallback + IntentClassifier references.")
print(f"New file has {len(result)} lines.")
