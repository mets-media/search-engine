-- FUNCTION: public.new_page_function()

-- DROP FUNCTION IF EXISTS public.new_page_function();

CREATE OR REPLACE FUNCTION public.new_page_function()
    RETURNS trigger
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE NOT LEAKPROOF
AS $BODY$
declare page_id integer;
declare lemma_id integer;

Declare lemmainfo Varchar(50);

declare new_lemma VarChar(50);
declare new_Count Int;
declare new_Rank real;
BEGIN

	INSERT INTO page(Site_id,Path, Code, Content)
    VALUES(new.Site_id,new.Path, new.Code, new.Content)
	Returning Id into page_id;

	for lemmainfo in select unnest(string_to_array(new.lemmatization,';'))
	loop
	    if (length(lemmainfo) > 0) then
	 		new_lemma = Split_Part(lemmaInfo,',',1);
	  		new_count = Cast(Split_Part(lemmaInfo,',',2) as Integer);
			new_rank  = Cast(Split_Part(lemmaInfo,',',3) as real);
        	--return next;

			insert into LEMMA (Site_Id, Lemma,Frequency, Rank)
			values (new.Site_Id,new_lemma,new_count,new_rank)
			on conflict on constraint siteId_lemma_unique do update set frequency = frequency + new_count
			returning id into lemma_id;

		end if;
	end loop;
    RETURN NULL;
END;

$BODY$;

ALTER FUNCTION public.new_page_function()
    OWNER TO postgres;
