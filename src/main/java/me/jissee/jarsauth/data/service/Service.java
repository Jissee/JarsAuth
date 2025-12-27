package me.jissee.jarsauth.data.service;

public interface Service {
    void initTable();
    default void inject(ServiceResolver resolver){}
}
