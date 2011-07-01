package com.cloud.projects.dao;

import javax.ejb.Local;

import com.cloud.projects.ProjectVO;
import com.cloud.utils.db.GenericDaoBase;

@Local(value={ProjectDao.class})
public class ProjectDaoImpl extends GenericDaoBase<ProjectVO, Long> implements ProjectDao {

}
