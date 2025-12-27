package me.jissee.jarsauth.data.service;

public interface ServiceResolver {
    <S extends Service> S getService(Class<S> clazz);
}
