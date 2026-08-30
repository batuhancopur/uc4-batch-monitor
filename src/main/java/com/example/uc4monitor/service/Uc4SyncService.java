package com.example.uc4monitor.service;

import com.example.uc4monitor.domain.Uc4JobDefinition;
import com.example.uc4monitor.domain.Uc4JobRunHistory;
import com.example.uc4monitor.repository.Uc4SourceRepository;
import com.example.uc4monitor.repository.Uc4TargetRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Uc4SyncService {

    private static final Logger log = LoggerFactory.getLogger(Uc4SyncService.class);

    private final Uc4SourceRepository sourceRepository;
    private final Uc4TargetRepository targetRepository;

    public Uc4SyncService(Uc4SourceRepository sourceRepository, Uc4TargetRepository targetRepository) {
        this.sourceRepository = sourceRepository;
        this.targetRepository = targetRepository;
    }

    @Transactional
    public void sync() {
        List<Uc4JobDefinition> definitions = sourceRepository.findTeamDefinitions();
        List<Uc4JobRunHistory> histories = sourceRepository.findRecentRunHistory();
        targetRepository.replaceDefinitionsAndHistory(definitions, histories);
        log.info("UC4 sync completed. definitions={}, histories={}", definitions.size(), histories.size());
    }
}
