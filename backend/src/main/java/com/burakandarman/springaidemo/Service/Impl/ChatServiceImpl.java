package com.burakandarman.springaidemo.Service.Impl;

import com.burakandarman.springaidemo.Dto.AudioResponseDto;
import com.burakandarman.springaidemo.Service.ChatService;
import com.burakandarman.springaidemo.Service.FileService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;

    private final OpenAiImageModel openAiImageModel;

    private final OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;

    private final OpenAiAudioSpeechModel openAiAudioSpeechModel;

    private final FileService fileService;

    private final String baseUrl;

    private final String contextPath;

    public ChatServiceImpl(ChatClient chatClient,
                           OpenAiImageModel openAiImageModel,
                           OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel,
                           OpenAiAudioSpeechModel openAiAudioSpeechModel,
                           FileService fileService,
                           @Value("${base-url}") String baseUrl,
                           @Value("${server.servlet.context-path}") String contextPath) {
        this.chatClient = chatClient;
        this.openAiImageModel = openAiImageModel;
        this.openAiAudioTranscriptionModel = openAiAudioTranscriptionModel;
        this.openAiAudioSpeechModel = openAiAudioSpeechModel;
        this.fileService = fileService;
        this.baseUrl = baseUrl;
        this.contextPath = contextPath;
    }

    @Override
    public String getTextResponse(String promptString) {

        return chatClient.prompt()
                .user(promptString)
                .call()
                .content();
    }

    @Override
    public String getImageResponse(String promptString) {

        return openAiImageModel.call(new ImagePrompt(promptString))
                .getResult()
                .getOutput()
                .getUrl();
    }

    @Override
    public AudioResponseDto getAudioResponse(MultipartFile promptAudio) throws IOException {

        String promptAudioFileName = "promptAudio.mp3";
        String audioResponseFileName = "audioResponse.mp3";

        File promptAudioFile = fileService.createFileFromBytes(promptAudio.getBytes(), promptAudioFileName);

        String promptString = openAiAudioTranscriptionModel
                .call(new AudioTranscriptionPrompt(new FileSystemResource(promptAudioFile)))
                .getResult()
                .getOutput();

        String textResponse = getTextResponse(promptString);

        byte[] audioResponseBytes = openAiAudioSpeechModel.call(new SpeechPrompt(textResponse))
                .getResult()
                .getOutput();

        fileService.createFileFromBytes(audioResponseBytes, audioResponseFileName);

        return new AudioResponseDto(
                baseUrl + contextPath + "/file/" + promptAudioFileName,
                promptString,
                 baseUrl + contextPath + "/file/" + audioResponseFileName,
                textResponse
        );

    }

}
