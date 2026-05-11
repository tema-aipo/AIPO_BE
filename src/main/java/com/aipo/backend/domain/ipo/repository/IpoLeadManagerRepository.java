package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoLeadManager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IpoLeadManagerRepository extends JpaRepository<IpoLeadManager, Long> {

    List<IpoLeadManager> findAllByStock_IdOrderByDisplayOrderAsc(Long stockId);

    @org.springframework.data.jpa.repository.Query("select m from IpoLeadManager m where m.stock.id in :stockIds order by m.displayOrder asc")
    List<IpoLeadManager> findAllByStock_IdIn(@org.springframework.data.repository.query.Param("stockIds") List<Long> stockIds);

    void deleteAllByStock_Id(Long stockId);
}
