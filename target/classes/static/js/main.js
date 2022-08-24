$(function(){

    var numberClick = 0;

    $('#test_bottom').click(function(){
            let generateText = $('<a class="textFromTestBottom">Вы нажали тестовую кнопку</a>');
            let inBox1 = $('.experiment');
            inBox1.append(generateText);
            $('#task-put-form').css({display: 'flex'});
        });

    const appendTask = function(data){
        var taskCode = '<a href="#" class="task-link" data-id="' +
            data.id + '">' + data.header + '</a><br>';
        $('#task-list')
            .append('<div>' + taskCode + '</div>');
    };

    //Loading Tasks on load page
//    $.get('/tasks/', function(response)
//    {
//        for(i in response) {
//            appendTask(response[i]);
//        }
//    });

    //Show adding Task form
    $('#show-add-task-form').click(function(){
        $('#task-form').css({display: 'flex'});
    });

//Closing adding Task form
    $('#task-form').click(function(event){
        if(event.target === this) {
            $(this).css({display: 'none'});
        }
    });

    $('#task-put-form').click(function(event){
        if(event.target === this) {
            $(this).css({display: 'none'});
            location.reload();
        }
    });

    //Getting Task
    $(document).on('click', '.task-link', function(){
        var link = $(this);
        var taskId = link.data('id');

        $.ajax({
            method: "GET",
            url: '/tasks/' + taskId,
            success: function(response)
            {

//                if(numberClick != taskId) {
                    var code = '<div class="task-one"><span>Плановый год завершения:' + response.yearTask +
                    '</span><p></p><button id="put-task" data-id="' + taskId +
                    '">Редактировать</button><p></p><button id="dell-task" data-id="' + taskId +
                    '">Удалить</button></div>';

                    link.parent().append(code);
                    numberClick = link.data('id');
//                }
                 $('#put-task').click(function(){
//                 getTaskElement($('#put-task').data('id'));
//                 $('body').append($('<a> Id' + $('#put-task').data('id') + '</a>'));
                   $('#task-put-form > form').html('');
                   let fillingTaskPutForm = '<h2>Редактирование дела</h2>';
                   fillingTaskPutForm += '<label>Название дела </label>';
                   fillingTaskPutForm += '<input type="text" name="header" value="' + response.header +
                   '">';
                   fillingTaskPutForm += '<label>Описание дела</label>';
                   fillingTaskPutForm += '<input type="text" name="description" value="' + response
                   .description + '">';
                   fillingTaskPutForm += '<label>Год срока:</label>';
                   fillingTaskPutForm += '<input type="text" name="yearTask" value="' + response.yearTask
                   + '">';
                   fillingTaskPutForm += '<label>Месяц срока:</label>';
                   fillingTaskPutForm += '<input type="text" name="monthTask" value="' + response
                   .monthTask + '">';
                   fillingTaskPutForm += '<label>День срока:</label>';
                   fillingTaskPutForm += '<input type="text" name="dayTask" value="' + response.dayTask +
                   '">';
                   fillingTaskPutForm += '<hr><button id="put-task-save" data-id="' + taskId +
                   '">Редактировать</button>';
                   $('#task-put-form > form').append(fillingTaskPutForm);
                   $('#task-put-form').css({display: 'flex'});
                   $('#put-task-save').click(function()
                       {
                           var data = $('#task-put-form form').serialize();
                           var link = $(this);
                           var taskId = link.data('id');
                           $.ajax({
                               method: "PUT",
                               url: '/tasks/' + taskId,
                               data: data,
                               success: function(response)
                               {
                                   $('#task-put-form').css('display', 'none');
                                   var task = {};
                                   task.id = response;
                                   var dataArray = $('#task-put-form form').serializeArray();
                                   for(i in dataArray) {
                                       Task[dataArray[i]['header']] = dataArray[i]['value'];
                                   }
                                   appendTask(task);
                               }
                           });
                           location.reload();
                           return false;
                       });
                });

                $('#dell-task').click(function(){
                   var link = $(this);
                   var taskId = link.data('id');
                   $.ajax({
                       method: "DELETE",
                       url: '/tasks/' + taskId,
                   });
                   location.reload();
                   return false;
                });
            },
            error: function(response)
            {
                if(response.status == 404) {
                    alert('Дело не найдено!');
                }
            }
        });
        return false;
    });



    //Adding Task
    $('#save-task').click(function()
    {
        var data = $('#task-form form').serialize();
        $.ajax({
            method: "POST",
            url: '/tasks/',
            data: data,
            success: function(response)
            {
                $('#task-form').css('display', 'none');
                var task = {};
                task.id = response;
                var dataArray = $('#task-form form').serializeArray();
                for(i in dataArray) {
                    Task[dataArray[i]['header']] = dataArray[i]['value'];
                }
                appendTask(task);
            }
        });
        return false;
    });
});