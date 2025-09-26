package com.orpe.consultants.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.orpe.consultants.service.BomDataService;
import com.orpe.consultants.repository.ModelsRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BomDataServiceImpl implements BomDataService {

}
