package com.aaax.usecase;

public interface UseCase<ID, Args> {


    default void execute(ID id, Args args) {

    }
}
