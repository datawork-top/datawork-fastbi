package top.datawork.fastbi.service;

import java.util.List;

public interface StatisticService {

    <T> void insert(List<T> durationInfos, Class clz);

}
