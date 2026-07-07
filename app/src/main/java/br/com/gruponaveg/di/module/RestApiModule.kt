package br.com.gruponaveg.di.module

import android.content.Context
import br.com.gruponaveg.services.network.cadastro.ConstantesService
import br.com.gruponaveg.services.network.cadastro.passagem.AgenciasService
import br.com.gruponaveg.services.network.cadastro.passagem.PessoaService
import br.com.gruponaveg.services.network.cadastro.passagem.VeiculoService
import br.com.gruponaveg.services.network.cadastro.viagem.EmpresaService
import br.com.gruponaveg.services.network.cadastro.viagem.NavioService
import br.com.gruponaveg.services.network.cadastro.viagem.ViagemService
import br.com.gruponaveg.services.network.faturamento.PassagemPassageiroService
import br.com.gruponaveg.services.network.faturamento.PassagemService
import br.com.gruponaveg.services.network.faturamento.PassagemVeiculoService
import br.com.gruponaveg.services.network.operacoes.UsuarioService
import br.com.gruponaveg.util.Fachada
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RestApiModule {

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BODY)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        @ApplicationContext context: Context,
    ): Retrofit {
        val baseUrl = Fachada.getProperties(context, "url").orEmpty()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(JacksonConverterFactory.create())
            .client(client)
            .build()
    }

    @Provides
    @Singleton
    fun provideUsuarioService(retrofit: Retrofit): UsuarioService {
        return retrofit.create(UsuarioService::class.java)
    }

    @Provides
    @Singleton
    fun provideConstantesConteudosService(retrofit: Retrofit): ConstantesService {
        return retrofit.create(ConstantesService::class.java)
    }

    @Provides
    @Singleton
    fun provideEmpresaService(retrofit: Retrofit): EmpresaService {
        return retrofit.create(EmpresaService::class.java)
    }

    @Provides
    @Singleton
    fun provideNavioService(retrofit: Retrofit): NavioService {
        return retrofit.create(NavioService::class.java)
    }

    @Provides
    @Singleton
    fun provideViagemService(retrofit: Retrofit): ViagemService {
        return retrofit.create(ViagemService::class.java)
    }

    @Provides
    @Singleton
    fun provideAgenteService(retrofit: Retrofit): AgenciasService {
        return retrofit.create(AgenciasService::class.java)
    }

    @Provides
    @Singleton
    fun providePassagemService(retrofit: Retrofit): PassagemService {
        return retrofit.create(PassagemService::class.java)
    }

    @Provides
    @Singleton
    fun providePessoaService(retrofit: Retrofit): PessoaService {
        return retrofit.create(PessoaService::class.java)
    }

    @Provides
    @Singleton
    fun providePassagemPassageiroService(retrofit: Retrofit): PassagemPassageiroService {
        return retrofit.create(PassagemPassageiroService::class.java)
    }

    @Provides
    @Singleton
    fun provideVeiculoService(retrofit: Retrofit): VeiculoService {
        return retrofit.create(VeiculoService::class.java)
    }

    @Provides
    @Singleton
    fun providePassagemVeiculoService(retrofit: Retrofit): PassagemVeiculoService {
        return retrofit.create(PassagemVeiculoService::class.java)
    }
}