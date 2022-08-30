package io.heachi.backend;

import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Getter
public class Response<T> {

  private static final int SUCCESS_CODE = 0;

  private final Integer code;
  private T payload;

  public Response(int code){
    this.code = code;
  }

  public Response<T> body(T payload){
    this.payload = payload;
    return this;
  }


  @Contract(value = " -> new", pure = true)
  public static <T> Response<T> ok(){
    return new Response<>(SUCCESS_CODE);
  }
}
