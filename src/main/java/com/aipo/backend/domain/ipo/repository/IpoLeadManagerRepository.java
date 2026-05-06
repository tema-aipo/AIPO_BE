package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoLeadManager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IpoLeadManagerRepository extends JpaRepository<IpoLeadManager, Long> {

    List<IpoLeadManager> findAllByStock_IdOrderByDisplayOrderAsc(Long stockId);

    void deleteAllByStock_Id(Long stockId);
}
